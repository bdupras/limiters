package com.duprasville.limiters.comms;

public interface MessageSender {
    void send(long src, long dst, Object msg);
    void sendAny(long src, long[] dst, Object msg);
    void receive(long src, long dst, Object msg);
    void onReceive(MessageReceiver receiver);

    @FunctionalInterface
    interface MessageReceiver {
        void apply(long src, long dst, Object msg);
    }
}
