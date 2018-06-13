package com.duprasville.limiters.treefill.domain;

public class CloseWindow extends BaseMessage {

  public CloseWindow(long src, long dst, long round) {
    super(src, dst, round, MessageType.CloseWindow);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" +
        "src=" + getSrc() +
        ", dst=" + getDst() +
        ", round=" + getRound() +
        '}';
  }
}
