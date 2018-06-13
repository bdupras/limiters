package com.duprasville.limiters.treefill;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.treefill.domain.TreeFillMessage;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import java.util.concurrent.*;

public class TreeFillRateLimiter implements DistributedRateLimiter {
    private final long nodeId;
    private final long clusterSize;
    private final Executor executor;
    private final MessageDeliverator messageDeliverator;

    private final Stopwatch stopwatch;
    private final ConcurrentMap<Long, WindowState> currentWindows = new ConcurrentSkipListMap<>();

    private volatile long permitsPerSecond;
    private final MessageDeliverator windowingDeliverator;

    public TreeFillRateLimiter(
            long id,
            long N,
            long W,
            Ticker ticker,
            Executor executor,
            MessageDeliverator messageDeliverator
    ) {
        this.nodeId = id;
        this.clusterSize = N;
        this.executor = executor;
        this.messageDeliverator = messageDeliverator;
        this.windowingDeliverator = (message) -> {
            ((TreeFillMessage)message).window = currentWindowFrame();
            return messageDeliverator.send(message);
        };

        this.permitsPerSecond = W;
        this.stopwatch = Stopwatch.createStarted(ticker);
    }
    public long getId() {
        return nodeId;
    }

    @Override
    public CompletableFuture<Boolean> acquire(long permits) {
        return currentWindow().acquire(permits);
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    @Override
    public CompletableFuture<Void> receive(Message message) {
        TreeFillMessage treefillMessage = (TreeFillMessage) message; // TODO validate inbound message first
        return getWindowFor(treefillMessage.window).receive(treefillMessage);
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

    private WindowState getWindowFor(long windowFrame) {
        long currentWindowFrame = currentWindowFrame();
        dropOldWindows(currentWindowFrame);
        if (allowedWindowFrame(currentWindowFrame, windowFrame)) {
            return currentWindows.computeIfAbsent(
                            windowFrame,
                            (frame) -> new WindowState(
                                    this.nodeId,
                                    this.clusterSize,
                                    this.permitsPerSecond,
                                    messageDeliverator
                            ));
        } else {
            return WindowState.NIL_WINDOW;
        }
    }

    WindowState currentWindow() {
        long currentWindowFrame = currentWindowFrame();
        return getWindowFor(currentWindowFrame);
    }
}
