package com.duprasville.limiters.comms;

@FunctionalInterface
public interface MessageReceiver {
    void apply(CommNode src, CommNode dst, Message msg);
}
