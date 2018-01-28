package com.duprasville.limiters.comms;

public interface CommNode {
    void onReceive(MessageReceiver recv);
    void deliver(CommNode src, CommNode dst, Message msg);
}
