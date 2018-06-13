package com.duprasville.limiters.treefill.domain;

import com.duprasville.limiters.api.Message;

public interface TreeFillMessage extends Message {
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
