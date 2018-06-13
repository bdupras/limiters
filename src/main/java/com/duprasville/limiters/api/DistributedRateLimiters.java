package com.duprasville.limiters.api;

import com.google.common.base.Ticker;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DistributedRateLimiters {
    public static final DistributedRateLimiter UNLIMITED = (permits) -> CompletableFuture.completedFuture(true);
    public static final DistributedRateLimiter NEVER = (permits) -> CompletableFuture.completedFuture(false);

    public static final DistributedRateLimiter treefill(
            TreeFillConfig treeFillConfig,
            Ticker ticker,
            Executor executor,
            MessageDeliverator messageDeliverator,
            Random random
    ) {
        return UNLIMITED;
    }

    public static final DistributedRateLimiter divided() {
        return NEVER;
    }
}
