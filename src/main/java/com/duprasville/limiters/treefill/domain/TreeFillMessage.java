package com.duprasville.limiters.treefill.domain;

import com.duprasville.limiters.api.Message;

public abstract class TreeFillMessage implements Message {
  private final long src;
  private final long dst;

  public final long round;
  public final MessageType type;
  public long window = -1L;

  @Override
  public long getSrc() {
    return src;
  }

  @Override
  public long getDst() {
    return dst;
  }

  TreeFillMessage(long src, long dst, long round, MessageType type) {
    this.src = src;
    this.dst = dst;
    this.round = round;
    this.type = type;
  }

  public enum MessageType {
    Acquire,
    CloseWindow,
    Detect,
    ChildFull,
    Inform,
    RoundFull
  }

}
