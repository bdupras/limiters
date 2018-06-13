package com.duprasville.limiters.testutil;

import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.api.Message;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class TestMessageSender implements MessageDeliverator {
    private final Random random;
    Function<Message, CompletableFuture<Void>> onSend = (msg) -> {
        return CompletableFuture.completedFuture(null);
    };

    public TestMessageSender(Random random) {
        this.random = random;
    }

    @Override
    public CompletableFuture<Void> send(Message message) {
        return onSend.apply(message);
    }

    public void onSend(Function<Message, CompletableFuture<Void>> onSend) {
        this.onSend = onSend;
    }

    //@Override
    //public long anyAvailableNode(long[] nodes) {
    //    return nodes[random.nextInt(nodes.length)];
    //}
}
