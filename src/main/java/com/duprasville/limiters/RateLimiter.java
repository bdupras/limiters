package com.duprasville.limiters;

public interface RateLimiter {
    boolean tryAcquire(int permits);

    default boolean tryAcquire() {
        return tryAcquire(1);
    }

    default void setRate(long permitsPerSecond) {
    }
}
