package com.duprasville.limiters.api;

public abstract class Message {
  private final long src;
  private final long dst;

  public final long round;
  public final MessageType type;
  public long window = -1L;

  public long getSrc() {
    return src;
  }

  public long getDst() {
    return dst;
  }

  protected Message(long src, long dst, long round, MessageType type) {
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
