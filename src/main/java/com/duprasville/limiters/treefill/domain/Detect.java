package com.duprasville.limiters.treefill.domain;

public class Detect extends TreeFillMessage {
  private final long permitsAcquired;

  public long getPermitsAcquired() {
    return this.permitsAcquired;
  }

  public Detect(long src, long dst, long round, long permitsAcquired) {
    super(src, dst, round, MessageType.Detect);
    this.permitsAcquired = permitsAcquired;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" +
        "src=" + getSrc() +
        ", dst=" + getDst() +
        ", round=" + round +
        ", permitsAcquired=" + permitsAcquired +
        '}';
  }
}
