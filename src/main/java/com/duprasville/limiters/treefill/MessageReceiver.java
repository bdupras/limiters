package com.duprasville.limiters.treefill;

import com.duprasville.limiters.api.Message;

@FunctionalInterface
public interface MessageReceiver {
    void receive(Message message);
}
