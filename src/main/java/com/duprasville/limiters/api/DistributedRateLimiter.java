package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

/**
 * Main interface for distributed rate limiting implementations to provide.
 */
@FunctionalInterface
public interface DistributedRateLimiter {
    CompletableFuture<Boolean> acquire(long permits);

    default CompletableFuture<Boolean> acquire() {
        return acquire(1L);
    }

    default CompletableFuture<Void> receive(Message message) {
        return CompletableFuture.completedFuture(null);
    }

    default void setRate(long permitsPerSecond) { }
}

