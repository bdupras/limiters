package com.duprasville.limiters.testutil;

import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.api.Message;

import java.util.Random;
import java.util.function.Consumer;

public class TestMessageSender implements MessageDeliverator {
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

    //@Override
    //public long anyAvailableNode(long[] nodes) {
    //    return nodes[random.nextInt(nodes.length)];
    //}
}
