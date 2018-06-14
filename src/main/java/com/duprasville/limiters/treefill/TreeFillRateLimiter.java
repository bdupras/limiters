package com.duprasville.limiters.treefill;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.treefill.domain.TreeFillMessage;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import java.util.concurrent.*;

public class TreeFillRateLimiter implements ClusterRateLimiter {
    private final long nodeId;
    private final long clusterSize;
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
            MessageDeliverator messageDeliverator
    ) {
        this.nodeId = id;
        this.clusterSize = N;
        this.messageDeliverator = messageDeliverator;
        this.windowingDeliverator = (message) -> {
            ((TreeFillMessage)message).window = currentWindowFrame();
            messageDeliverator.send(message);
        };

        this.permitsPerSecond = W;
        this.stopwatch = Stopwatch.createStarted(ticker);
    }
    public long getId() {
        return nodeId;
    }

    @Override
    public boolean acquire(long permits) {
        return currentWindow().acquire(permits);
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    @Override
    public void receive(Message message) {
        TreeFillMessage treefillMessage = (TreeFillMessage) message; // TODO validate inbound message first

        // TODO this is a nasty dirty hack - stop it.
        if (treefillMessage.window == -1) {
            treefillMessage.window = currentWindowFrame();
        }

        getWindowFor(treefillMessage.window).receive(treefillMessage);
    }

    private long currentWindowFrame() {
        return stopwatch.elapsed(TimeUnit.SECONDS);
    }

    private void dropOldWindows(long currentWindowFrame) {
        currentWindows
                .keySet()
                .stream()
                .filter(frame -> !allowedWindowFrame(currentWindowFrame, frame))
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
                                    this.messageDeliverator
                            ));
        } else {
            return WindowState.NIL;
        }
    }

    WindowState currentWindow() {
        long currentWindowFrame = currentWindowFrame();
        return getWindowFor(currentWindowFrame);
    }
}
