package com.duprasville.limiters.api;

@FunctionalInterface
public interface MessageReceiver {
  MessageReceiver NIL = (m) -> {};
  void receive(Message message);
}
