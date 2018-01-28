package com.duprasville.limiters.comms;

@FunctionalInterface
public interface TestMessageOnSend {
    void apply(long src, long dst, Object msg);
}
