package com.duprasville.limiters.comms;

public interface Communicator {
    void sendTo(CommNode src, CommNode dst, Message msg);
    CommNode getCommNodeById(long nodeId);
    Message newMessage(Object payload);
}
