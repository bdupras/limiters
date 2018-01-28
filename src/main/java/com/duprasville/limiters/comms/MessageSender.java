package com.duprasville.limiters.comms;

public interface MessageSender {
    void send(long src, long dst, Object msg);
    void sendAny(long src, long[] dst, Object msg);
}
