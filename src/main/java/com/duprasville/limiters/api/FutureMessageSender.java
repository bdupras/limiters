package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface FutureMessageSender {
  FutureMessageSender NIL = (message -> CompletableFuture.completedFuture(null));
  CompletableFuture<Void> send(Message message);
}
