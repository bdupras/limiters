package com.duprasville.limiters.treefill;

import com.duprasville.limiters.ClusterRateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.AtomicMaxLongIncrementor;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.annotations.VisibleForTesting;

import static com.duprasville.limiters.treefill.TreeFillMath.nodePermitsToDetectPerRound;
import static java.lang.Math.max;
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
        if (ret) {
            messageSource.send(new Inform(nodeConfig.clusterNodeId, nodeConfig.parentNodeId, format("tryAcquire(%d) success", permits)));
        }
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
        System.out.println(detect);
    }
}

class WindowState {
    final WindowConfig windowConfig;
    final AtomicMaxLongIncrementor currentRound;
    final MessageSource messageSource;
    final AtomicMaxLongIncrementor permitsAcquired;
    final AtomicMaxLongIncrementor pendingPermitsToSignalDetect = new AtomicMaxLongIncrementor(0L, Long.MAX_VALUE);
    final NodeConfig nodeConfig;


    public WindowState(WindowConfig windowConfig, MessageSource messageSource) {
        this.windowConfig = windowConfig;
        this.nodeConfig = windowConfig.nodeConfig;

        // TODO - should a node continue to allow requests until it receives a message from the root node to close the door?
        this.permitsAcquired = new AtomicMaxLongIncrementor(0L, windowConfig.clusterPermits);

        // rounds start at 1 to match the paper
        this.currentRound = new AtomicMaxLongIncrementor( 1L, windowConfig.getRounds()-1);
        this.messageSource = messageSource;
    }

    public long advanceRound() {
        currentRound.tryIncrement(1L);
        return getCurrentRound();
    }

    public long getCurrentRound() {
        return currentRound.get();
    }

    public boolean tryAcquire(long permits) {
        // when window is closed (due to signal from root node), return false
        // push requested permits onto pending
        // drain pending into a list of Detect messages
        // send all Detect messages to base nodes sendAny(from, Set<to>, List<Detect>)

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
                // TODO send the Detect messages to the base nodes, not the parent
                messageSource.send(new Detect(nodeConfig.clusterNodeId, nodeConfig.parentNodeId, currRound, permitsToDetect));
                currRound = advanceRound(); // causes node to independently auto-advance through rounds - may want to make this an option/strategy
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
        this.isBaseNode = levelId != baseLevelId;

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
- get this working for W <= N
- send Detect(window, round, permits detected) to base layer (Set<nodeId> or nodeId[])
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