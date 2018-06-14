package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;

@FunctionalInterface
public interface FutureMessageSender {
  FutureMessageSender NIL = (message -> CompletableFuture.completedFuture(null));
  CompletableFuture<Void> send(Message message);
}
