package com.duprasville.limiters.comms;

import java.util.function.Consumer;

public class TestMessageSource implements MessageSource {
    Consumer<Message> onSend = (msg) -> {};

    @Override
    public void send(Message message) {
        onSend.accept(message);
    }

    public void onSend(Consumer<Message> onSend) {
        this.onSend = onSend;
    }
}
