package com.duprasville.limiters.api;

import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.util.SerialExecutor;

public class SerialDeliverator implements MessageDeliverator {
  final SerialExecutor serialExecutor;
  public SerialDeliverator(ExecutorService executorService) {
    this.serialExecutor = new SerialExecutor(executorService);
  }

  private MessageReceiver messageReceiver = MessageReceiver.NIL;
  public void onReceive(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  @Override
  public void receive(Message message) {
    serialExecutor.execute(() -> messageReceiver.receive(message));
  }

  private MessageSender messageSender = MessageSender.NIL;
  public void onSend(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  @Override
  public void send(Message message) {
    messageSender.send(message);
  }
}
