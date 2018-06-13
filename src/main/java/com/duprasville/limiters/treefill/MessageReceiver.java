package com.duprasville.limiters.treefill;

import com.duprasville.limiters.treefill.domain.Message;

@FunctionalInterface
public interface MessageReceiver {
    void receive(Message message);
}
