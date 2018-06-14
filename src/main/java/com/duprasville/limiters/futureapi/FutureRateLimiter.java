package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface FutureRateLimiter {
  CompletableFuture<Boolean> acquire(long permits);

  default CompletableFuture<Boolean> acquire() {
    return acquire(1L);
  }

  default void setRate(long permitsPerSecond) { }
}
