package com.duprasville.limiters.treefill;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageSender;
import com.duprasville.limiters.treefill.domain.TreeFillMessage;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;

public class TreeFillRateLimiter implements ClusterRateLimiter {
  private final long nodeId;
  private final long clusterSize;

  private final Stopwatch stopwatch;
  private final ConcurrentMap<Long, WindowState> currentWindows = new ConcurrentSkipListMap<>();

  private volatile long permitsPerSecond;
  private final MessageSender windowingSender;

  public TreeFillRateLimiter(
      long id,
      long N,
      long W,
      Ticker ticker,
      MessageSender messageSender
  ) {
    this.nodeId = id;
    this.clusterSize = N;
    this.windowingSender = (message) -> {
      ((TreeFillMessage) message).window = currentWindowFrame();
      messageSender.send(message);
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
        .forEach((oldWindow) ->
            currentWindows.remove(oldWindow)
        );
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
              this.windowingSender
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
