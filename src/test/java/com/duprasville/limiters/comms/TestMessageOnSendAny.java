package com.duprasville.limiters.comms;

@FunctionalInterface
public interface TestMessageOnSendAny {
    void apply(long src, long[] dst, Object msg);
}
