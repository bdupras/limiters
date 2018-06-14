package com.duprasville.limiters.api;

public interface MessageDeliverator extends MessageSender, MessageReceiver {
  MessageDeliverator NIL = new MessageDeliverator() {
    @Override
    public void receive(Message message) { }

    @Override
    public void send(Message message) { }
  };

  @Override
  default void receive(Message message) {};
}
