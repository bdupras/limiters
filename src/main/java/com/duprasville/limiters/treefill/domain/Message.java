package com.duprasville.limiters.treefill.domain;

public interface Message {
  long getSrc();

  long getDst();

  long getRound();

  MessageType getType();

   enum MessageType {
    Acquire,
    CloseWindow,
    Detect,
    ChildFull,
    Inform,
    RoundFull
  }
}
