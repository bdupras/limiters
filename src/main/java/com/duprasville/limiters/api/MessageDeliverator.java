package com.duprasville.limiters.api;

public interface MessageDeliverator {
    void send(Message message);
    //long anyAvailableNode(long[] nodes);
}
