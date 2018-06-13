package com.duprasville.limiters.testutil;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TestMessageDeliverator implements MessageDeliverator {
    Function<Message, CompletableFuture<Void>> onSend = (msg) -> CompletableFuture.completedFuture(null);

    @Override
    public CompletableFuture<Void> send(Message message) {
        return onSend.apply(message);
    }

    public void onSend(Function<Message, CompletableFuture<Void>> onSend) {
        this.onSend = onSend;
    }
}
