package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageSender;

@FunctionalInterface
public interface FutureMessageSender {
  FutureMessageSender NIL = (message -> CompletableFuture.completedFuture(null));

  CompletableFuture<Void> send(Message message);

  static MessageSender toMessageSender(FutureMessageSender futureMessageSender) {
    return futureMessageSender::send;
  }
}
