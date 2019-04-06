package com.duprasville.limiters.treefill;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageSender;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

public class TreeFillRateLimiter implements ClusterRateLimiter {
    private final long nodeId;
    private final long clusterSize;

    private final Stopwatch stopwatch;
    private final ConcurrentMap<Long, WindowState> currentWindows = new ConcurrentSkipListMap<>();

    private volatile long permitsPerSecond;
    private final MessageSender windowingSender;

    private final boolean useRandomizedWindowState;

    public TreeFillRateLimiter(
        long selfNodeId, //id
        long numberOfNodes, //N
        long windowSizeInPermits, //W
        Ticker ticker,
        MessageSender messageSender
    ) {
        this(
            selfNodeId,
            numberOfNodes,
            windowSizeInPermits,
            ticker,
            messageSender,
            false);
    }

    public TreeFillRateLimiter(
        long selfNodeId, //id
        long numberOfNodes, //N
        long windowSizeInPermits, //W
        Ticker ticker,
        MessageSender messageSender,
        boolean useRandomizedWindowState
    ) {
        this.nodeId = selfNodeId;
        this.clusterSize = numberOfNodes;
        this.windowingSender = (message) -> {
            message.window = currentWindowFrame();
            messageSender.send(message);
        };

        this.permitsPerSecond = windowSizeInPermits;
        this.stopwatch = Stopwatch.createStarted(ticker);
        this.useRandomizedWindowState = useRandomizedWindowState;
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
        // TODO this is a nasty dirty hack - stop it.
        if (message.window == -1) {
            message.window = currentWindowFrame();
        }

        getWindowFor(message.window).receive(message);
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
                (frame) -> createWindowState());
        } else {
            return WindowState.NIL;
        }
    }

    private WindowState createWindowState() {
        if (this.useRandomizedWindowState) {
            return new WindowStateWithRandomizedRedirects(
                this.nodeId,
                this.clusterSize,
                this.permitsPerSecond,
                this.windowingSender
            );
        } else {
            return new WindowState(
                this.nodeId,
                this.clusterSize,
                this.permitsPerSecond,
                this.windowingSender
            );
        }
    }

    WindowState currentWindow() {
        long currentWindowFrame = currentWindowFrame();
        return getWindowFor(currentWindowFrame);
    }
}
