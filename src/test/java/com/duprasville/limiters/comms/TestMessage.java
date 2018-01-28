package com.duprasville.limiters.comms;

public class TestMessage implements Message {
    private final Object payload;

    TestMessage(Object payload) {
        this.payload = payload;
    }

    @Override
    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "TestMessage{" +
                "payload=" + payload +
                '}';
    }
}
