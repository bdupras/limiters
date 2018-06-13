package com.duprasville.limiters.treefill.domain;

import com.duprasville.limiters.api.Message;

abstract class BaseMessage implements Message {
  private final long src;
  private final long dst;
  private final long round;
  private MessageType type;

  @Override
  public long getSrc() {
    return src;
  }

  @Override
  public long getDst() {
    return dst;
  }

  @Override
  public long getRound() {
    return round;
  }

  @Override
  public MessageType getType() {
    return type;
  }

  BaseMessage(long src, long dst, long round, MessageType type) {
    this.src = src;
    this.dst = dst;
    this.round = round;
    this.type = type;
  }
}
