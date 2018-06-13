package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

public interface MessageDeliverator {
    CompletableFuture<Void> send(Message message);
}
