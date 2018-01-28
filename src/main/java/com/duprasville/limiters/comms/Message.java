package com.duprasville.limiters.comms;

public class Message {
    private final Object payload;

    Message(Object payload) {
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "Message{" +
                "payload=" + payload +
                '}';
    }
}
