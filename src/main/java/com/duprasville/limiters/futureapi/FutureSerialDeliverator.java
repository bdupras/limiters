package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageReceiver;
import com.duprasville.limiters.util.SerialExecutor;

public class FutureSerialDeliverator implements FutureMessageDeliverator {
  final SerialExecutor serialExecutor;
  public FutureSerialDeliverator(ExecutorService executorService) {
    this.serialExecutor = new SerialExecutor(executorService);
  }

  private MessageReceiver messageReceiver = MessageReceiver.NIL;
  public void onReceive(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  @Override
  public CompletableFuture<Void> receive(Message message) {
    return CompletableFuture.runAsync(() -> messageReceiver.receive(message), serialExecutor);
  }

  private FutureMessageSender futureMessageSender = FutureMessageSender.NIL;
  public void onSend(FutureMessageSender futureMessageSender) {
    this.futureMessageSender = futureMessageSender;
  }

  @Override
  public CompletableFuture<Void> send(Message message) {
    return futureMessageSender.send(message);
  }
}
