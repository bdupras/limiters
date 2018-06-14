package com.duprasville.limiters.api;

@FunctionalInterface
public interface MessageSender {
  MessageSender NIL = (m) -> {};
  void send(Message message);
}
