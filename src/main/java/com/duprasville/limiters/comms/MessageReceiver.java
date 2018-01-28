package com.duprasville.limiters.comms;

@FunctionalInterface
public interface MessageReceiver {
    void apply(Node src, Node dst, Message msg);
}
