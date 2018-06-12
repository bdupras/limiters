package com.duprasville.limiters.treefill;

public interface MessageSender {
    void send(Message message);
    long anyAvailableNode(long[] nodes);
}
