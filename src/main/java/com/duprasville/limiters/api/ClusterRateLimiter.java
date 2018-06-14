package com.duprasville.limiters.api;

public interface ClusterRateLimiter extends SimpleRateLimiter, MessageReceiver {
  @Override
  default void receive(Message message) {
  }
}
