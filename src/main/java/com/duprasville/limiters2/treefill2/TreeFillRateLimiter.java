package com.duprasville.limiters2.treefill2;

import com.duprasville.limiters2.util.KaryTree;
import com.duprasville.limiters2.DistributedRateLimiter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;

public class TreeFillRateLimiter implements DistributedRateLimiter, MessageReceiver {
    private final long nodeId;
    private final long clusterSize;
    private final KaryTree karyTree;
    private final Executor executor;
    private final MessageSender messageSender;
    private final Random random;

    private final Stopwatch stopwatch;
    private final ConcurrentMap<Long, WindowState> currentWindows = new ConcurrentSkipListMap<>();

    private final NodeConfig nodeConfig;

    private volatile WindowConfig windowConfig;

    public TreeFillRateLimiter(
            KaryTree karyTree,
            long nodeId,
            long clusterSize,
            long permitsPerSecond,
            Ticker ticker,
            Executor executor,
            MessageSender messageSender,
            Random random
    ) {
        this.karyTree = karyTree;
        this.nodeId = nodeId;
        this.clusterSize = clusterSize;
        this.executor = executor;
        this.messageSender = messageSender;
        this.random = random;

        this.nodeConfig = new NodeConfig(karyTree, nodeId, clusterSize);
        this.windowConfig = new WindowConfig(nodeConfig, permitsPerSecond);
        this.stopwatch = Stopwatch.createStarted(ticker);
    }

    @Override
    public boolean tryAcquire(long permits) {
        return currentWindow().tryAcquire(permits);
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.windowConfig = new WindowConfig(nodeConfig, permitsPerSecond);
    }

    @Override
    public void receive(Message message) {
        getWindowFor(message.getWindow()).ifPresent(w -> w.receive(message));
    }

    private long currentWindowFrame() {
        return stopwatch.elapsed(TimeUnit.SECONDS);
    }

    private void dropOldWindows(long currentWindowFrame) {
        currentWindows
                .keySet()
                .stream()
                .filter(frame -> allowedWindowFrame(currentWindowFrame, frame))
                .forEach(currentWindows::remove);
    }

    private boolean allowedWindowFrame(long currentWindowFrame, long subjectWindowFrame) {
        long minFrame = currentWindowFrame - 2L;
        long maxFrame = currentWindowFrame + 2L;
        return (subjectWindowFrame >= minFrame) && (subjectWindowFrame <= maxFrame);
    }

    private Optional<WindowState> getWindowFor(long windowFrame) {
        long currentWindowFrame = currentWindowFrame();
        dropOldWindows(currentWindowFrame);
        if (allowedWindowFrame(currentWindowFrame, windowFrame)) {
            return Optional.of(
                    currentWindows.computeIfAbsent(
                            windowFrame,
                            (frame) -> new WindowState(
                                    windowConfig,
                                    executor,
                                    messageSender,
                                    frame
                            )));
        } else {
            return Optional.empty();
        }

    }

    private WindowState currentWindow() {
        long currentWindowFrame = currentWindowFrame();
        return getWindowFor(currentWindowFrame).get();
    }
}
