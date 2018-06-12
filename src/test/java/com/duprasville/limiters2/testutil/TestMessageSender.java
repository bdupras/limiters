package com.duprasville.limiters2.testutil;

import com.duprasville.limiters2.treefill2.MessageSender;
import com.duprasville.limiters2.treefill2.Message;

import java.util.Random;
import java.util.function.Consumer;

public class TestMessageSender implements MessageSender {
    private final Random random;
    Consumer<Message> onSend = (msg) -> {};

    public TestMessageSender(Random random) {
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
