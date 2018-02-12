package com.duprasville.limiters.comms;

import java.util.function.Consumer;

public class TestMessageSource implements MessageSource {
    Consumer<Message> onSend = (msg) -> {};

    public TestMessageSource() {
    }


    @Override
    public void send(Message message) {
        onSend.accept(message);
    }

    public void onSend(Consumer<Message> onSend) {
        this.onSend = onSend;
    }

    @Override
    public long anyAvailableNode(long[] nodes) {
        return 0;
    }
}
