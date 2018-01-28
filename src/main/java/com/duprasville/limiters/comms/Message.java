package com.duprasville.limiters.comms;

@FunctionalInterface
public interface Message {
    Object getPayload();
}
