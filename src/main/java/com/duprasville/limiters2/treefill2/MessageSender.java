package com.duprasville.limiters2.treefill2;

public interface MessageSender {
    void send(Message message);
    long anyAvailableNode(long[] nodes);
}
