package com.duprasville.limiters.api;

import com.duprasville.limiters.treefill.TreeFillRateLimiter;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ForkedRateLimiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DistributedRateLimiters {
  public static final DistributedRateLimiter UNLIMITED = (permits) -> CompletableFuture.completedFuture(true);
  public static final DistributedRateLimiter NEVER = (permits) -> CompletableFuture.completedFuture(false);

  public static DistributedRateLimiter treefill(
      TreeFillConfig treeFillConfig,
      Ticker ticker,
      Executor executor,
      MessageDeliverator messageDeliverator
  ) {
    return new TreeFillRateLimiter(treeFillConfig.nodeId, treeFillConfig.clusterSize, treeFillConfig.permitsPerSecond, ticker, executor, messageDeliverator);
  }

  public static DistributedRateLimiter divided(
      DividedConfig config,
      Ticker ticker
  ) {
    double localPermitsPerSecond = config.permitsPerSecond / (double) config.clusterSize;
    final ForkedRateLimiter forkedRateLimiter = ForkedRateLimiter.create(localPermitsPerSecond, ticker);

    return new DistributedRateLimiter() {
      @Override
      public CompletableFuture<Boolean> acquire(long permits) {
        return CompletableFuture.completedFuture(forkedRateLimiter.tryAcquire((int) permits));
      }

      @Override
      public void setRate(long permitsPerSecond) {
        forkedRateLimiter.setRate(permitsPerSecond);
      }
    };
  }
}
