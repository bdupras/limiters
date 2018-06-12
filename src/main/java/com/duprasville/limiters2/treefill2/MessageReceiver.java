package com.duprasville.limiters2.treefill2;

@FunctionalInterface
public interface MessageReceiver {
    void receive(Message message);
}
