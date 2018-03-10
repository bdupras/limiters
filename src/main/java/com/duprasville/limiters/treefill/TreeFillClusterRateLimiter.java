package com.duprasville.limiters.treefill;

import com.duprasville.limiters.ClusterRateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TreeFillClusterRateLimiter implements ClusterRateLimiter, TreeFillMessageSink {
    @VisibleForTesting
    NodeConfig nodeConfig;

    @VisibleForTesting
    WindowConfig windowConfig;

    @VisibleForTesting
    AtomicReference<WindowState> currentWindow;
    AtomicLong currentWindowFrame;
    final long clusterSize;
    final KaryTree karyTree;
    final MessageSource messageSource;
    final Stopwatch stopwatch;
    final Random random;

    public TreeFillClusterRateLimiter(
            long permitsPerSecond,
            long nodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSource messageSource,
            Ticker ticker,
            Random random) {
        this.clusterSize = clusterSize;
        this.karyTree = karyTree;
        this.messageSource = messageSource;
        this.stopwatch = Stopwatch.createStarted(ticker);
        this.random = random;

        reconfigure(nodeId, clusterSize, karyTree);
        setRate(permitsPerSecond);
        currentWindowFrame = new AtomicLong(0L);
        currentWindow = new AtomicReference<>(new WindowState(currentWindowFrame.get(), windowConfig, messageSource));
    }

    public TreeFillClusterRateLimiter(
            long permitsPerSecond,
            long nodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSource messageSource) {
        this(permitsPerSecond,
                nodeId,
                clusterSize,
                karyTree,
                messageSource,
                Ticker.systemTicker(),
                new Random()
        );
    }

    private void reconfigure(long clusterNodeId, long clusterSize, KaryTree karyTree) {
        nodeConfig = new NodeConfig(karyTree, clusterNodeId, clusterSize);
    }

    @VisibleForTesting
    WindowState currentWindow() {
        WindowState oldWindow = currentWindow.get();
        long newWindowFrame = stopwatch.elapsed(TimeUnit.SECONDS);
        long oldWindowFrame = currentWindowFrame.getAndAccumulate(newWindowFrame, (currWin, newWin) -> newWin);

        if (newWindowFrame > oldWindowFrame) {
            boolean didAdvance = currentWindow.compareAndSet(
                    oldWindow,
                    new WindowState(currentWindowFrame.get(), windowConfig, messageSource)
            );
            // TODO ?? carry forward oldWindow.pendingPermitsToDetect ?
//            if (didAdvance && nodeConfig.isRootNode) {
//                long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
//                WindowState currentWin = currentWindow.get();
//                System.out.println(format(
//                        "Time %d, Node %d, Window %d, CPA %d, NPA %d, advance to Window %d, CPA %d, NPA %d",
//                        millis,
//                        nodeConfig.nodeId,
//                        oldWindow.windowId,
//                        oldWindow.getClusterPermitsAcquired(),
//                        oldWindow.getNodePermitsAcquired(),
//                        currentWin.windowId,
//                        currentWin.getClusterPermitsAcquired(),
//                        currentWin.getNodePermitsAcquired()
//                ));
//            }
        }

        return currentWindow.get();
    }

    @Override
    public boolean tryAcquire(long permits) {
        return currentWindow().tryAcquire(permits);
    }

    @Override
    public void setRate(long permitsPerSecond) {
        windowConfig = new WindowConfig(nodeConfig, permitsPerSecond, random);
    }

    @Override
    public void receive(Inform inform) {
        System.out.println(inform);
    }

    @Override
    public void receive(Detect detect) {
        //Straggling Detects from a previous window can count towards the current window
        currentWindow().receive(detect);
    }

    @Override
    public void receive(Full full) {
        // Discard straggling Full messages from previous windows
        WindowState currWindow = currentWindow();
        if (full.window == currWindow.windowId) {
            currentWindow().receive(full);
        }
    }

    @Override
    public void receive(WindowFull windowFull) {
        // Discard straggling WindowFull messages from previous windows
        WindowState currWindow = currentWindow();
        if (windowFull.window == currWindow.windowId) {
            currentWindow().receive(windowFull);
        }
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

inject from environment
-----------------------
- messaging provider (done)
- random provider
- ticker / time provider
- metrics consumer
- logging consumer

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
