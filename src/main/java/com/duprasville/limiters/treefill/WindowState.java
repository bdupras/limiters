package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.AtomicMaxLongIncrementor;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.max;

class WindowState {
    final long windowId;
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
    private final ConcurrentHashMap<Long, Full> fullsSent = new ConcurrentHashMap<>();

    public WindowState(long windowId, WindowConfig windowConfig, MessageSource messageSource) {
        this.windowId = windowId;
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
            Detect detect = new Detect(nodeConfig.nodeId, nodeConfig.nodeId, windowId, currRound, permitsToDetect);
            forward(messageSource.anyAvailableNode(nodeConfig.leafNodes), detect);
            currRound = nodeCurrentRound.get();
            permitsToDetect = max(1L, windowConfig.getPermitsToDetectPerRound(currRound));
        }
    }

    private void addToClusterPermitsAcquiredAndCloseWindowIfNeeded(long permitsAcquired) {
        long pa = clusterPermitsAcquired.addAndGet(permitsAcquired);
        if (pa >= windowConfig.clusterPermits && !clusterWindowFull.get()) {
            receive(new WindowFull(nodeConfig.nodeId, nodeConfig.nodeId, windowId, clusterPermitsAcquired.get()));
        }
    }

    public void receive(Detect detect) {
        if (!(tryAbsorbIntoCurrentRound(detect) // any node can absorb detected permits
                || tryForwardDown(detect)       // root & inner nodes can pass Detects downward
                || tryStoreLocally(detect)      // leaf nodes store Detects locally
                || tryForwardUp(detect)         // leaf & inner nodes can pass Detects up when full
        )) {
            // root node received a Detect and all its children have reported Full
            addToClusterPermitsAcquiredAndCloseWindowIfNeeded(detect.permitsAcquired);
        }
    }

    private boolean tryStoreLocally(Detect detect) {
        boolean stored = detectsTable.tryPut(detect);

        // seems the sim environment drops packets, sometimes causing a Full to be sent but not received?
        // this is actually cool, since the real world drops packets
        if (!stored && detectsTable != DetectTable.NIL) {
            long permitsAcquired = detectsTable.getRow(detect.round).stream().mapToLong(d -> d.permitsAcquired).sum();
            ensureFullSentToParent(detect.round, permitsAcquired);
        }
        return stored;
    }

    private void ensureFullSentToParent(long round, long permitsFilled) {
        Full full = fullsSent.computeIfAbsent(round, r -> new Full(
                nodeConfig.nodeId,
                nodeConfig.parentNodeId,
                windowId,
                round,
                permitsFilled
        ));
        messageSource.send(full);
    }

    private void onDetectTableRoundFull(long round, List<Detect> detects) {
        nodeCurrentRound.tryIncrement(1L);

        long permitsAcquired = detects.stream().mapToLong(d -> d.permitsAcquired).sum();
        ensureFullSentToParent(round, permitsAcquired);
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
            ensureFullSentToParent(round, permitsFilled);
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
        messageSource.send(new Detect(nodeConfig.nodeId, dst, detect.window, detect.round, detect.permitsAcquired));
    }

    private void forward(long dst, Full full) {
        messageSource.send(new Full(nodeConfig.nodeId, dst, full.window, full.round, full.permitsAcquired));
    }

    private void forward(long dst, WindowFull windowFull) {
        messageSource.send(new WindowFull(nodeConfig.nodeId, dst, windowFull.window, windowFull.permitsAcquired));
    }

    private boolean tryAbsorbIntoCurrentRound(Detect detect) {
        if (detect.round <= nodeCurrentRound.get()) return false;
        addToPendingPermitsAndSendDetectsIfNeeded(detect.permitsAcquired);
        return true;
    }

    private boolean tryForwardDown(Detect detect) {
        return fullsReceived.tryWithFirstEmpty(detect.round, child ->
                forward(child, detect)
        );
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

    public long getClusterPermitsAcquired() {
        return clusterPermitsAcquired.get();
    }

    public long getNodePermitsAcquired() {
        return nodePermitsAcquired.get();
    }
}
