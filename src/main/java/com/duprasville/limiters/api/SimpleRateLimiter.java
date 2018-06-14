package com.duprasville.limiters.api;

@FunctionalInterface
public interface SimpleRateLimiter {
  boolean acquire(long permits);

  default boolean acquire() {
    return acquire(1L);
  }

  default void setRate(long permitsPerSecond) { }
}
