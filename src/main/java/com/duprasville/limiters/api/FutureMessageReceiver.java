package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface FutureMessageReceiver {
  FutureMessageReceiver NIL = (message -> CompletableFuture.completedFuture(null));
  CompletableFuture<Void> receive(Message message);
}
