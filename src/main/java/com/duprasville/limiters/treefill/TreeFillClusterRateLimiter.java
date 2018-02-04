package com.duprasville.limiters.treefill;

import com.duprasville.limiters.ClusterRateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.AtomicMap;
import com.duprasville.limiters.util.AtomicMaxLongIncrementor;
import com.duprasville.limiters.util.AtomicTable;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.annotations.VisibleForTesting;

import java.util.OptionalLong;
import java.util.function.Consumer;

import static com.duprasville.limiters.treefill.TreeFillMath.nodePermitsToDetectPerRound;
import static java.lang.Math.max;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static java.util.Arrays.stream;

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
        currentWindow.receive(detect);
    }

    @Override
    public void receive(Full full) {
        currentWindow.receive(full);
    }
}

class DetectTable extends AtomicTable<Long, Long, Detect> {
    private final static Long FIRST = 1L;
    private final static Long SECOND = 2L;

    public boolean tryPut(Detect detect) {
        return tryPut(detect.round, FIRST, detect) || tryPut(detect.round, SECOND, detect);
    }

    public boolean isFull(long round) {
        return hasValue(round, FIRST) && hasValue(round, SECOND);
    }

    public long permitsDetected(long round) {
        return valuesAt(round).stream().mapToLong(d -> d.permitsAcquired).sum();
    }
}

class ChildFullTable extends AtomicTable<Long, Long, Full> {
    private final long[] childNodeIds;

    public ChildFullTable(long[] childNodeIds) {
        this.childNodeIds = childNodeIds;
    }

    public boolean tryAnyChildWithSpace(long round, Consumer<Long> action) {
        OptionalLong childWithSpace = stream(childNodeIds)
                .filter(childId -> !hasValue(round, childId))
                .findAny();

        // Java Optional is awkward.
        if (childWithSpace.isPresent()) {
            childWithSpace.ifPresent(action::accept);
            return true;
        } else {
            return false;
        }
    }

    public boolean isFull(long round) {
        return !stream(childNodeIds)
                .filter(childId -> !hasValue(round, childId))
                .findAny()
                .isPresent();

    }

    public long permitsFilled(long round) {
        return valuesAt(round).stream().mapToLong(full -> full.permitsFilled).sum();
    }
}

class WindowState {
    final WindowConfig windowConfig;
    final AtomicMaxLongIncrementor currentRound;
    final MessageSource messageSource;
    final AtomicMaxLongIncrementor permitsAcquired;
    final AtomicMaxLongIncrementor pendingPermitsToSignalDetect = new AtomicMaxLongIncrementor(0L, Long.MAX_VALUE);
    final NodeConfig nodeConfig;
    final DetectTable roundDetectTable = new DetectTable();
    final AtomicMap<Long, Full> roundFullMap = new AtomicMap<>();
    final ChildFullTable childFullTable;

    public WindowState(WindowConfig windowConfig, MessageSource messageSource) {
        this.windowConfig = windowConfig;
        this.nodeConfig = windowConfig.nodeConfig;
        this.permitsAcquired = new AtomicMaxLongIncrementor(0L, windowConfig.clusterPermits);
        // rounds start at 1 to match the paper
        this.currentRound = new AtomicMaxLongIncrementor(1L, windowConfig.getRounds() - 1);
        this.messageSource = messageSource;
        this.childFullTable = new ChildFullTable(nodeConfig.childNodeIds);
    }

    public long advanceRound() {
        currentRound.tryIncrement(1L);
        return getCurrentRound();
    }

    public long getCurrentRound() {
        return currentRound.get();
    }

    public void receive(Detect detect) {
        if (nodeConfig.isRootNode) {
            forwardToChildWithSpaceOrElseAddToPermitsDetected(detect);
        } else if (nodeConfig.isInnerNode) {
            forwardToChildWithSpaceOrElseForwardToParent(detect);
        } else if (nodeConfig.isBaseNode) {
            storeOrElseForwardToLevelAbove(detect);
        }
    }

    private void forwardToChildWithSpaceOrElseAddToPermitsDetected(Detect detect) {
        if (!tryForwardToChildWithSpace(detect)) {
            System.out.println("TODO root's children are full - nowhere to forward: " + detect);
        }
    }

    private void forwardToChildWithSpaceOrElseForwardToParent(Detect detect) {
        if (!tryForwardToChildWithSpace(detect)) {
            forward(nodeConfig.parentNodeId, detect);
        }
    }

    private void storeOrElseForwardToLevelAbove(Detect detect) {
        if (roundDetectTable.tryPut(detect)) {
            ifDetectTableIsFullSendFull(detect.round);
        } else {
            forward(messageSource.anyAvailableNode(nodeConfig.parentLevelNodeIds), detect);
        }
    }

    private boolean tryForwardToChildWithSpace(Detect detect) {
        return childFullTable.tryAnyChildWithSpace(detect.round, childId -> forward(childId, detect));
    }

    private void forward(long dst, Detect detect) {
        messageSource.send(new Detect(nodeConfig.clusterNodeId, dst, detect.round, detect.permitsAcquired));
    }

    private void ifDetectTableIsFullSendFull(long round) {
        if (roundDetectTable.isFull(round) &&
                roundFullMap.tryPut(round, () -> new Full(
                        nodeConfig.clusterNodeId,
                        nodeConfig.parentNodeId,
                        round,
                        roundDetectTable.permitsDetected(round)
                ))) {
            // base node just filled up and needs to send exactly one Full message to its parent
            messageSource.send(roundFullMap.get(round));
        }
    }

    public void receive(Full full) {
        if (nodeConfig.isRootNode) {
            storeAndCheckForEndOfRound(full);
        } else if (nodeConfig.isInnerNode) {
            storeOrThrowUnexpected(full);
        } else if (nodeConfig.isBaseNode) {
            throw new RuntimeException("wut - base nodes should not receive fulls: " + full);
        }
    }

    private void storeAndCheckForEndOfRound(Full full) {
        if (tryStore(full)) {
            ifChildrenAreFullSendEndOfRound(full.round);
        } else {
            System.out.println(format("TODO Root node detected round %d is full, should signal end of round ", full.round));
        }

    }

    private void ifChildrenAreFullSendEndOfRound(long round) {
        if (childFullTable.isFull(round)) {
            // TODO keep a tally and possibly carry over the overage into the next window?
            System.out.println("TODO root node should tally permits acquired and accumulate excesses - perhaps pasing excesses to the next window reducing the next window's W by the excess amount");
            throw new RuntimeException("TODO - just breaking here for now.");
        }
    }

    private void storeOrThrowUnexpected(Full full) {
        if (tryStore(full)) {
            ifChildrenAreFullSendFull(full.round);
        } else {
            throw new RuntimeException("wut - I received too many full messages: " + full);
        }
    }

    private boolean tryStore(Full full) {
        return childFullTable.tryPut(full.round, full.src, full);
    }

    private void ifChildrenAreFullSendFull(long round) {
        if (childFullTable.isFull(round) &&
                roundFullMap.tryPut(round, () -> new Full(
                        nodeConfig.clusterNodeId,
                        nodeConfig.parentNodeId,
                        round,
                        childFullTable.permitsFilled(round)
                ))) {
            // inner node just filled up and needs to send exactly one Full message to its parent
            messageSource.send(roundFullMap.get(round));
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