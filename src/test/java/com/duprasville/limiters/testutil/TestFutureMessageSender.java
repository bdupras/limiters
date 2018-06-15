package com.duprasville.limiters.testutil;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.futureapi.FutureMessageSender;

public class TestFutureMessageSender implements FutureMessageSender {
    Function<Message, CompletableFuture<Void>> onSend = (msg) -> CompletableFuture.completedFuture(null);

    @Override
    public CompletableFuture<Void> send(Message message) {
      return onSend.apply(message);
    }

    public void onSend(Function<Message, CompletableFuture<Void>> onSend) {
        this.onSend = onSend;
    }
}
