package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;

@FunctionalInterface
public interface FutureMessageReceiver {
  FutureMessageReceiver NIL = (message -> CompletableFuture.completedFuture(null));
  CompletableFuture<Void> receive(Message message);
}
