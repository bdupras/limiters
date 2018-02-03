package com.duprasville.limiters.comms;

public interface MessageSource {
    void send(Message message);
    long anyAvailableNode(long[] nodes);
}
