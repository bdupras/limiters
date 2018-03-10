package com.duprasville.limiters.comms;

import java.util.Random;
import java.util.function.Consumer;

public class TestMessageSource implements MessageSource {
    final Random random;
    Consumer<Message> onSend = (msg) -> {};

    public TestMessageSource(Random random) {
        this.random = random;
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
        return nodes[random.nextInt(nodes.length)];
    }
}
