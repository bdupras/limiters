package com.duprasville.limiters.treefill.domain;

import com.duprasville.limiters.api.Message;

public class CloseWindow extends Message {

  public CloseWindow(long src, long dst, long round) {
    super(src, dst, round, MessageType.CloseWindow);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "{" +
        "src=" + getSrc() +
        ", dst=" + getDst() +
        ", round=" + round +
        '}';
  }
}
