package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface DistributedRateLimiter extends FutureRateLimiter, FutureMessageReceiver {
  @Override
  default CompletableFuture<Void> receive(Message message) {
    return CompletableFuture.completedFuture(null);
  }
}
