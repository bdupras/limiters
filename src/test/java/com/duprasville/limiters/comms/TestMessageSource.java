package com.duprasville.limiters.comms;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;

public class TestMessageSource implements MessageSource {
    Random random = new Random();
    Consumer<Message> onSend = (msg) -> {};
    Function<long[], Long> onAnyNode = (nodes) -> nodes[random.nextInt(nodes.length)];

    public TestMessageSource() {
    }

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
        return 0;
    }

    public long onAnyNode(long[] nodes) {
        return onAnyNode.apply(nodes);
    }
}
