package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageReceiver;

@FunctionalInterface
public interface FutureMessageReceiver {
  FutureMessageReceiver NIL = (message -> CompletableFuture.completedFuture(null));

  CompletableFuture<Void> receive(Message message);

  static FutureMessageReceiver fromMessageReceiver(
      MessageReceiver messageReceiver,
      ExecutorService executorService
  ) {
    return (message) ->
        CompletableFuture.runAsync(() -> messageReceiver.receive(message), executorService);
  }
}
