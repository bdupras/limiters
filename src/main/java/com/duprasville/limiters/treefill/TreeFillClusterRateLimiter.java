package com.duprasville.limiters.treefill;

import com.duprasville.limiters.ClusterRateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.AtomicMaxLongIncrementor;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.max;

public class TreeFillClusterRateLimiter implements ClusterRateLimiter, TreeFillMessageSink {
    @VisibleForTesting
    NodeConfig nodeConfig;

    @VisibleForTesting
    WindowConfig windowConfig;

    @VisibleForTesting
    WindowState currentWindow;

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

    @Override
    public void receive(WindowFull windowFull) {
        currentWindow.receive(windowFull);
    }
}

class WindowState {
    private final NodeConfig nodeConfig;
    private final WindowConfig windowConfig;
    private final MessageSource messageSource;
    private final AtomicMaxLongIncrementor nodeCurrentRound;
    private final AtomicLong clusterPermitsAcquired;
    private final AtomicBoolean clusterWindowFull = new AtomicBoolean(false);
    private final AtomicMaxLongIncrementor nodePermitsAcquired;
    private final AtomicMaxLongIncrementor pendingPermitsToDetect = new AtomicMaxLongIncrementor(0L, Long.MAX_VALUE);
    private final DetectTable detectsTable;
    private final FullTable fullsReceived;
    ;

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
    }

    public boolean tryAcquire(long permits) {
        // allow acquire when
        //   1. no signal has been received at this node to close the window
        //   2. this node's acquired permits plus the requested permits is fewer than the whole cluster's permits

        if (!clusterWindowFull.get() && nodePermitsAcquired.tryIncrement(permits)) {
            addToPendingPermitsAndSendDetectsIfNeeded(permits);
            return true;
        }
        return false;
    }

    private void addToPendingPermitsAndSendDetectsIfNeeded(long permitsAcquired) {
        pendingPermitsToDetect.tryIncrement(permitsAcquired);
        long currRound = nodeCurrentRound.get();
        // If permits to detect this round is 0, send one Detect for every permit acquired. This happens later
        // rounds when the cluster-wide permits to detect for the round is less than number of cluster nodes.
        long permitsToDetect = max(1L, windowConfig.getPermitsToDetectPerRound(currRound));
        while (pendingPermitsToDetect.tryDecrement(permitsToDetect)) {
            Detect detect = new Detect(nodeConfig.nodeId, nodeConfig.nodeId, currRound, permitsToDetect);
            forward(messageSource.anyAvailableNode(nodeConfig.leafNodes), detect);
            currRound = nodeCurrentRound.get();
            permitsToDetect = max(1L, windowConfig.getPermitsToDetectPerRound(currRound));
        }
    }

    private void addToClusterPermitsAcquiredAndCloseWindowIfNeeded(long permitsAcquired) {
        long pa = clusterPermitsAcquired.addAndGet(permitsAcquired);
        if (pa >= windowConfig.clusterPermits && !clusterWindowFull.get()) {
            receive(new WindowFull(nodeConfig.nodeId, nodeConfig.nodeId, clusterPermitsAcquired.get()));
        }
    }

    public void receive(Detect detect) {
        if (!(tryAbsorbIntoCurrentRound(detect) // any node can absorb detected permits
                || tryForwardDown(detect)       // root & inner nodes can pass downward
                || detectsTable.tryPut(detect)  // leaf nodes can store locally
                || tryForwardUp(detect)         // leaf & inner nodes can pass up when full
        )) {
            // root node received a Detect and all its children have reported Full
            addToClusterPermitsAcquiredAndCloseWindowIfNeeded(detect.permitsAcquired);
        }
    }

    private void onDetectTableRoundFull(long round, List<Detect> detects) {
        nodeCurrentRound.tryIncrement(1L);

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
            addToClusterPermitsAcquiredAndCloseWindowIfNeeded(full.permitsAcquired);
        }
    }

    private void onFullTableRoundFull(long round, List<Full> fulls) {
        nodeCurrentRound.tryIncrement(1L);

        if (!nodeConfig.isRootNode) {
            long permitsFilled = fulls.stream().mapToLong(f -> f.permitsAcquired).sum();
            tryForwardUp(new Full(
                    nodeConfig.nodeId,
                    nodeConfig.parentNodeId,
                    round,
                    permitsFilled
            ));
        }
    }

    public void receive(WindowFull windowFull) {
        if (clusterWindowFull.compareAndSet(false, true)) {
            for (long child : nodeConfig.children) {
                forward(child, windowFull);
            }
        }
    }

    private void forward(long dst, Detect detect) {
        messageSource.send(new Detect(nodeConfig.nodeId, dst, detect.round, detect.permitsAcquired));
    }

    private void forward(long dst, Full full) {
        messageSource.send(new Full(nodeConfig.nodeId, dst, full.round, full.permitsAcquired));
    }

    private void forward(long dst, WindowFull windowFull) {
        messageSource.send(new WindowFull(nodeConfig.nodeId, dst, windowFull.permitsAcquired));

    }

    private boolean tryAbsorbIntoCurrentRound(Detect detect) {
        if (detect.round > nodeCurrentRound.get()) {
            addToPendingPermitsAndSendDetectsIfNeeded(detect.permitsAcquired);
            return true;
        }
        return false;
    }

    private boolean tryForwardDown(Detect detect) {
        return fullsReceived.tryWithFirstEmpty(detect.round, child -> {
            forward(child, detect);
        });
    }

    private boolean tryForwardUp(Detect detect) {
        if (nodeConfig.isRootNode) return false;
        forward(messageSource.anyAvailableNode(nodeConfig.parentLevelNodeIds), detect);
        return true;
    }

    private boolean tryForwardUp(Full full) {
        if (nodeConfig.isRootNode) return false;
        forward(nodeConfig.parentNodeId, full);
        return true;
    }
}


/*
TODO next
---------
- pre-fills (so last rounds don't allow too many, and for W <= N)
- window advance
- back-pressure?
- straggler bug
- carry over overages from old window to new?


Simulation features
-------------------
- metrics: MsgLoad, MaxRecv, MaxSend
- Rejects vs Accepts
- add centralized impl (memcached sim)


Refactorings
------------
- Make DetectTree/DetectTreeNode separate & distinct from rate limiting logic
- Make tryWithFirstEmpty efficient
*/
