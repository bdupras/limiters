package com.duprasville.limiters.treefill;

import com.duprasville.limiters.ClusterRateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.AtomicMaxLongIncrementor;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import static com.duprasville.limiters.treefill.TreeFillMath.nodePermitsToDetectPerRound;
import static java.lang.Math.max;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;

public class TreeFillClusterRateLimiter implements ClusterRateLimiter, TreeFillMessageSink {
    private NodeConfig nodeConfig;

    public NodeConfig getNodeConfig() {
        return nodeConfig;
    }

    private WindowConfig windowConfig;

    public WindowConfig getWindowConfig() {
        return windowConfig;
    }

    private WindowState currentWindow;

    public WindowState getCurrentWindow() {
        return currentWindow;
    }

    final long nodeId;
    final long clusterSize;
    final KaryTree karyTree;
    final MessageSource messageSource;


    public TreeFillClusterRateLimiter(
            long permitsPerSecond,
            long nodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSource messageSource
    ) {
        this.nodeId = nodeId;
        this.clusterSize = clusterSize;
        this.karyTree = karyTree;
        this.messageSource = messageSource;
        reconfigure(nodeId, clusterSize, karyTree);
        setRate(permitsPerSecond);
        advanceWindow();
    }

    private void reconfigure(long clusterNodeId, long clusterSize, KaryTree karyTree) {
        this.nodeConfig = new NodeConfig(karyTree, clusterNodeId, clusterSize);
    }

    @VisibleForTesting
    void advanceWindow() {
        // TODO: carry over overages from old window to new?
        this.currentWindow = new WindowState(windowConfig, messageSource);
    }

    @Override
    public boolean tryAcquire(long permits) {
        return currentWindow.tryAcquire(permits);
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
        currentWindow.receive(detect);
    }

    @Override
    public void receive(Full full) {
        currentWindow.receive(full);
    }
}

class WindowState {
    private final NodeConfig nodeConfig;
    private final WindowConfig windowConfig;
    private final MessageSource messageSource;
    private final AtomicMaxLongIncrementor nodeCurrentRound;
    private final AtomicLong clusterPermitsAcquired;
    private final AtomicMaxLongIncrementor nodePermitsAcquired;
    private final AtomicMaxLongIncrementor pendingPermitsToDetect = new AtomicMaxLongIncrementor(0L, Long.MAX_VALUE);
    private final DetectTable detectsTable;
    private final FullTable fullsReceived;

    public WindowState(WindowConfig windowConfig, MessageSource messageSource) {
        this.windowConfig = windowConfig;
        this.nodeConfig = windowConfig.nodeConfig;
        this.detectsTable = windowConfig.detectTableTemplate.copy(this::onDetectTableRoundFull);
        this.fullsReceived = windowConfig.fullTableTemplate.copy(this::onFullTableRoundFull);

        this.clusterPermitsAcquired = new AtomicLong(0L);
        this.nodePermitsAcquired = new AtomicMaxLongIncrementor(0L, windowConfig.clusterPermits);
        // rounds start at 1 to match the paper
        this.nodeCurrentRound = new AtomicMaxLongIncrementor(1L, windowConfig.rounds.length);
        this.messageSource = messageSource;
        // TODO calculate pre-fills
    }

    public boolean tryAcquire(long permits) {
        // TODO check here for a signal from the root node that closed this window
        // allow acquire when
        //   1. no signal has been received at this node to close the window (TODO)
        //   2. this node's acquired permits plus the requested permits is fewer than the whole cluster's permits

        if (nodePermitsAcquired.tryIncrement(permits)) {
            sendDetectsForPendingPermits(permits);
            return true;
        }
        return false;
    }

    private void sendDetectsForPendingPermits(long permits) {
        pendingPermitsToDetect.tryIncrement(permits);
        long currRound = nodeCurrentRound.get();
        // when cluster permits < nodes in the cluster, some nodes' "fair share" will be 0.
        // if this is such a node, presume that we should send one Detect for every permit issued.
        long permitsToDetect = max(1L, windowConfig.getPermitsToDetectPerRound(currRound));
        while (pendingPermitsToDetect.tryDecrement(permitsToDetect)) {
            Detect detect = new Detect(nodeConfig.nodeId, nodeConfig.nodeId, currRound, permitsToDetect);
            forward(messageSource.anyAvailableNode(nodeConfig.baseNodeIds), detect);
            currRound = nodeCurrentRound.incrementAndGet();
            permitsToDetect = max(1L, windowConfig.getPermitsToDetectPerRound(currRound));
        }
    }

    public void receive(Detect detect) {
        if (!(tryForwardDown(detect)           // root & inner nodes can pass downward
                || detectsTable.tryPut(detect) // leaf nodes can store locally
                || tryForwardUp(detect)        // leaf & inner nodes can pass up when full
        )) {
            // root node received a Detect and all its children have reported Full
            acquireClusterPermits(detect.permitsAcquired);
        }
    }

    private void acquireClusterPermits(long permitsAcquired) {
        if (clusterPermitsAcquired.addAndGet(permitsAcquired) >= windowConfig.clusterPermits) {
            throw new RuntimeException("Should send EndWindow here");
        }

    }

    private void onDetectTableRoundFull(long round, List<Detect> detects) {
        long permitsAcquired = detects.stream().mapToLong(d -> d.permitsAcquired).sum();
        messageSource.send(new Full(
                nodeConfig.nodeId,
                nodeConfig.parentNodeId,
                round,
                permitsAcquired
        ));
    }

    public void receive(Full full) {
        if (!fullsReceived.tryPut(full)) tryForwardUp(full);
        if (nodeConfig.isRootNode) {
            acquireClusterPermits(full.permitsFilled);
        }
    }

    private void onFullTableRoundFull(long round, List<Full> fulls) {
        long permitsFilled = fulls.stream().mapToLong(f -> f.permitsFilled).sum();
        messageSource.send(new Full(
                nodeConfig.nodeId,
                nodeConfig.parentNodeId,
                round,
                permitsFilled
        ));
    }

    private boolean tryForwardDown(Detect detect) {
        return fullsReceived.tryWithEmpty(detect.round, child -> forward(child, detect));
    }

    private boolean tryForwardUp(Detect detect) {
        if (nodeConfig.isRootNode) return false;
        forward(messageSource.anyAvailableNode(nodeConfig.parentLevelNodeIds), detect);
        return true;
    }

    private boolean tryForwardUp(Full full) {
        if (nodeConfig.isRootNode) return false;
        forward(messageSource.anyAvailableNode(nodeConfig.parentLevelNodeIds), full);
        return true;
    }

    private void forward(long dst, Detect detect) {
        messageSource.send(new Detect(nodeConfig.nodeId, dst, detect.round, detect.permitsAcquired));
    }

    private void forward(long dst, Full full) {
        messageSource.send(new Detect(nodeConfig.nodeId, dst, full.round, full.permitsFilled));
    }
}


/**
 * Describes this node and its neighborhood within a virtual k-ary tree structure.
 */
class NodeConfig {
    final KaryTree karyTree;
    final long nodeId;
    final long clusterSize;

    final long levelId;
    final long parentNodeId;
    final long parentLevelId;
    final long[] parentLevelNodeIds;
    final long[] children;
    final long baseLevelId;
    final long[] baseNodeIds;

    final boolean isRootNode;
    final boolean isInnerNode;
    final boolean isBaseNode;

    NodeConfig(KaryTree karyTree, long nodeId, long clusterSize) {
        this.karyTree = karyTree;
        this.nodeId = nodeId;
        this.clusterSize = clusterSize;

        this.levelId = karyTree.levelOfNode(nodeId);
        this.parentNodeId = karyTree.parentOfNode(nodeId);
        this.parentLevelId = karyTree.levelOfNode(parentNodeId);
        this.parentLevelNodeIds = karyTree.nodesOfLevel(parentLevelId);
        this.baseLevelId = karyTree.getBaseLevel();
        this.baseNodeIds = karyTree.nodesOfLevel(this.baseLevelId);

        this.isRootNode = nodeId == parentNodeId;
        this.isBaseNode = levelId == baseLevelId;
        this.isInnerNode = !this.isBaseNode && !this.isRootNode;

        // TODO: have the tree return empty[] instead of IDs beyond the tree's capacity
        this.children = this.isBaseNode ? new long[]{} : karyTree.childrenOfNode(nodeId);
    }
}

class WindowConfig {
    final NodeConfig nodeConfig;
    final long[] rounds;
    final long[] nodePermitsPerRound;
    final long clusterPermits;
    final DetectTable detectTableTemplate;
    public FullTable fullTableTemplate;

    WindowConfig(NodeConfig nodeConfig, long clusterPermits) {
        this.nodeConfig = nodeConfig;
        this.clusterPermits = clusterPermits;
        this.nodePermitsPerRound = nodePermitsToDetectPerRound(clusterPermits, nodeConfig.nodeId, nodeConfig.clusterSize);

        this.rounds = LongStream.range(1, nodePermitsPerRound.length).toArray(); // [1L, 2L, ...]
        this.detectTableTemplate = nodeConfig.isBaseNode ? new DetectTable(rounds) : DetectTable.NIL;
        this.fullTableTemplate = nodeConfig.isBaseNode ? FullTable.NIL : new FullTable(rounds, nodeConfig.children);
    }

    long getPermitsToDetectPerRound(long round) {
        return nodePermitsPerRound[toIntExact(round)];
    }
}

/*
To do next
+ get this working for W <= N
+ send Detect(window, round, permits detected) to base layer (Set<nodeId> or nodeId[])
+ implement Full(window, round, permits detected) logic
- root node signals Stop(window)
*/

/*
Refactorings
So many arrays - it's ok to use List<> and Set<>, Brian
Make DetectTree/DetectTreeNode separate & distinct from rate limiting logic
create RootNode, InnerNode, and BaseNode specializations? - definitely!
*/