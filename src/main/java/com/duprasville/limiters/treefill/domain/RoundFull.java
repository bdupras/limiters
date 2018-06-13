package com.duprasville.limiters.treefill.domain;

public class RoundFull extends BaseMessage {
  final long round;

  public RoundFull(long src, long dst, long round) {
    super(src, dst, round, MessageType.RoundFull);
    this.round = round;
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
