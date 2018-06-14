package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;

@FunctionalInterface
public interface DistributedRateLimiter extends FutureRateLimiter, FutureMessageReceiver {
  @Override
  default CompletableFuture<Void> receive(Message message) {
    return CompletableFuture.completedFuture(null);
  }
}
