package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

/**
 * acquire() is used by clients to ask for one permit
 * receive(message) is used by us to enqueue a message for processing.
 */
public interface DistributedRateLimiter {

    CompletableFuture<Boolean> acquire();

    CompletableFuture<Void> receive(Message message);

    Long getId();
}

