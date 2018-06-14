package com.duprasville.limiters.testutil;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;

import java.util.function.Consumer;

public class TestMessageDeliverator implements MessageDeliverator {
    Consumer<Message> onSend = (msg) -> {};

    @Override
    public void send(Message message) {
        onSend.accept(message);
    }

    public void onSend(Consumer<Message> onSend) {
        this.onSend = onSend;
    }
}
