package com.duprasville.limiters.treefill;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.treefill.domain.Message;

/**
 * acquire() is used by clients to ask for one permit
 * receive(message) is used by us to enqueue a message for processing.
 */
interface Node {

    CompletableFuture<Boolean> acquire();

    CompletableFuture<Boolean> receive(Message message);

    Long getId();
}

