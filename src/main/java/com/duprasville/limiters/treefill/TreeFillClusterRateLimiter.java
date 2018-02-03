package com.duprasville.limiters.treefill;

import com.duprasville.limiters.ClusterRateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.AtomicMaxLongIncrementor;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.annotations.VisibleForTesting;

import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicReference;

import static com.duprasville.limiters.treefill.TreeFillMath.nodePermitsToDetectPerRound;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;

public class TreeFillClusterRateLimiter implements ClusterRateLimiter, TreeFillMessageSink {
    private NodeConfig nodeConfig;
    private WindowConfig windowConfig;

    private final MessageSource messageSource;
    private WindowState currentWindow;

    public TreeFillClusterRateLimiter(
            long permitsPerSecond,
            long clusterNodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSource messageSource
    ) {
        this.messageSource = messageSource;
        reconfigure(clusterNodeId, clusterSize, karyTree);
        setRate(permitsPerSecond);
        advanceWindow();
    }

    private void reconfigure(long clusterNodeId, long clusterSize, KaryTree karyTree) {
        this.nodeConfig = new NodeConfig(karyTree, clusterNodeId, clusterSize);
    }

    @VisibleForTesting
    void advanceWindow() {
        this.currentWindow = new WindowState(windowConfig, messageSource);
    }

    @Override
    public boolean tryAcquire(long permits) {
        boolean ret = currentWindow.tryAcquire(permits);
//        if (ret) {
//            messageSource.send(new Inform(nodeConfig.clusterNodeId, nodeConfig.parentNodeId, format("tryAcquire(%d) success", permits)));
//        }
        return ret;
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.windowConfig = new WindowConfig(nodeConfig, permitsPerSecond);
    }

    @Override
    public void receive(Inform inform) {
        System.out.println(inform);
    }

    @Override
    public void receive(Detect detect) {
        currentWindow.detect(detect);
    }
}

class AtomicReferenceGrid<T> {
    private final AtomicReference<T>[][] grid;
    public final int xLength;
    public final int yLength;

    @SuppressWarnings("unchecked")
    public AtomicReferenceGrid(int xdim, int ydim) {
        this.grid = (AtomicReference<T>[][]) Array.newInstance(AtomicReference.class, xdim, ydim);
        this.xLength = xdim;
        this.yLength = ydim;
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                grid[x][y] = new AtomicReference<T>();
            }
        }
    }

    public AtomicReference<T> get(int x, int y) {
        return grid[x][y];
    }

}

class DetectArray {
    final AtomicReferenceGrid<Detect> detects;

    DetectArray(int rounds) {
        this.detects = new AtomicReferenceGrid<>(2, rounds);
    }

    public boolean tryDetect(Detect detect) {
        // remember rounds are 1-based, but the underlying grid is 0-based
        int round = toIntExact(detect.round);
        return detects.get(0, round).compareAndSet(null, detect) ||
                detects.get(1, round).compareAndSet(null, detect);
    }
}

class WindowState {
    final WindowConfig windowConfig;
    final AtomicMaxLongIncrementor currentRound;
    final MessageSource messageSource;
    final AtomicMaxLongIncrementor permitsAcquired;
    final AtomicMaxLongIncrementor pendingPermitsToSignalDetect = new AtomicMaxLongIncrementor(0L, Long.MAX_VALUE);
    final NodeConfig nodeConfig;
    final DetectArray detects;

    public WindowState(WindowConfig windowConfig, MessageSource messageSource) {
        this.windowConfig = windowConfig;
        this.nodeConfig = windowConfig.nodeConfig;
        this.permitsAcquired = new AtomicMaxLongIncrementor(0L, windowConfig.clusterPermits);
        // rounds start at 1 to match the paper
        this.currentRound = new AtomicMaxLongIncrementor(1L, windowConfig.getRounds() - 1);
        this.messageSource = messageSource;
        this.detects = new DetectArray(toIntExact(windowConfig.getRounds()));
    }

    public long advanceRound() {
        currentRound.tryIncrement(1L);
        return getCurrentRound();
    }

    public long getCurrentRound() {
        return currentRound.get();
    }

    public void detect(Detect detect) {
//        System.out.println(format("Detect received (isBaseNode = %s) %s ", nodeConfig.isBaseNode, detect));
        if (nodeConfig.isBaseNode) {
            if (!detects.tryDetect(detect)) {
                // base node is full for the round specified by the Detect message. Forward to a node in the layer above.
                long nodeAbove = messageSource.anyAvailableNode(nodeConfig.parentLevelNodeIds);
                messageSource.send(new Detect(nodeConfig.clusterNodeId, nodeAbove, detect.round, detect.permitsAcquired));
            }
        } else if (nodeConfig.isInnerNode) {

        }
    }

    public boolean tryAcquire(long permits) {
        // #OGodMyEyes - separate acquisition from sending of detects

        // TODO check here for a signal from the root node that closed this window
        // allow acquire when
        //   1. no signal has been received at this node to close the window (TODO)
        //   2. this node's acquired permits plus the requested permits is fewer than the whole cluster's permits

        if (permitsAcquired.tryIncrement(permits)) {
            pendingPermitsToSignalDetect.tryIncrement(permits);
            long currRound = getCurrentRound();
            // when cluster permits < nodes in the cluster, some nodes' "fair share" will be 0.
            // if this is such a node, presume that we should send one Detect for every permit issued.
            long permitsToDetect = max(1L, windowConfig.getPermitsToDetectPerRound(currRound));

            while (pendingPermitsToSignalDetect.tryDecrement(permitsToDetect)) {
                long baseNode = messageSource.anyAvailableNode(nodeConfig.baseNodeIds);
                messageSource.send(new Detect(nodeConfig.clusterNodeId, baseNode, currRound, permitsToDetect));
                // causes node to independently auto-advance through rounds - may want to make this an option/strategy
                currRound = advanceRound();
                permitsToDetect = max(1L, windowConfig.getPermitsToDetectPerRound(currRound));
            }
            return true;
        } else {
            return false;
        }
    }
}


/**
 * Describes this node and its neighborhood within a virtual k-ary tree structure.
 */
class NodeConfig {
    final KaryTree karyTree;
    final long clusterNodeId;
    final long clusterSize;

    final long levelId;
    final long parentNodeId;
    final long parentLevelId;
    final long[] parentLevelNodeIds;
    final long[] childNodeIds;
    final long baseLevelId;
    final long[] baseNodeIds;

    final boolean isRootNode;
    final boolean isInnerNode;
    final boolean isBaseNode;

    NodeConfig(KaryTree karyTree, long clusterNodeId, long clusterSize) {
        this.karyTree = karyTree;
        this.clusterNodeId = clusterNodeId;
        this.clusterSize = clusterSize;

        this.levelId = karyTree.levelOfNode(clusterNodeId);
        this.parentNodeId = karyTree.parentOfNode(clusterNodeId);
        this.parentLevelId = karyTree.levelOfNode(parentNodeId);
        this.parentLevelNodeIds = karyTree.nodesOfLevel(parentLevelId);
        this.baseLevelId = karyTree.getBaseLevel();
        this.baseNodeIds = karyTree.nodesOfLevel(this.baseLevelId);

        this.isRootNode = clusterNodeId == parentNodeId;
        this.isBaseNode = levelId == baseLevelId;
        this.isInnerNode = !this.isBaseNode && !this.isRootNode;

        // TODO: have the tree return empty[] instead of IDs beyond the tree's capacity
        this.childNodeIds = this.isBaseNode ? new long[]{} : karyTree.childrenOfNode(clusterNodeId);
    }
}

class WindowConfig {
    final NodeConfig nodeConfig;
    final long[] permitsToDetectPerRound;
    final long clusterPermits;

    WindowConfig(NodeConfig nodeConfig, long clusterPermits) {
        this.nodeConfig = nodeConfig;
        this.clusterPermits = clusterPermits;
        this.permitsToDetectPerRound = nodePermitsToDetectPerRound(clusterPermits, nodeConfig.clusterNodeId, nodeConfig.clusterSize);
    }

    long getRounds() {
        return permitsToDetectPerRound.length;
    }

    long getPermitsToDetectPerRound(long round) {
        return permitsToDetectPerRound[toIntExact(round)];
    }
}

/*


To do next
+ get this working for W <= N
+ send Detect(window, round, permits detected) to base layer (Set<nodeId> or nodeId[])
- implement Full(window, round, permits detected) logic
- root node signals Stop(window)
*/

/*
Refactorings
Should a node continue to allow requests until it receives a message from the root node to close the door? (yes)
So many arrays - it's ok to use List<> and Set<>, Brian
Make DetectTree/DetectTreeNode separate & distinct from rate limiting logic
create RootNode, InnerNode, and BaseNode specializations?
 */