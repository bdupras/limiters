package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

public interface MessageDeliverator extends MessageSender, FutureMessageReceiver {
  MessageDeliverator NIL = new MessageDeliverator() {
    @Override
    public CompletableFuture<Void> receive(Message message) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void send(Message message) { }
  };

  @Override
  default CompletableFuture<Void> receive(Message message) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  void send(Message message);
}
