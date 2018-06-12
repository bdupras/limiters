package com.duprasville.limiters.treefill;

@FunctionalInterface
public interface MessageReceiver {
    void receive(Message message);
}
