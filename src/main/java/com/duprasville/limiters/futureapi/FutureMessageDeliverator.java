package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;

public interface FutureMessageDeliverator extends FutureMessageSender, FutureMessageReceiver {
  FutureMessageDeliverator NIL = new FutureMessageDeliverator() {
    @Override
    public CompletableFuture<Void> receive(Message message) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> send(Message message) {
      return CompletableFuture.completedFuture(null);
    }
  };
}
