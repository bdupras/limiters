package com.duprasville.limiters.api;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.duprasville.limiters.treefill.GenericNode;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ForkedRateLimiter;

public class DistributedRateLimiters {
  public static final DistributedRateLimiter UNLIMITED = (permits) -> CompletableFuture.completedFuture(true);
  public static final DistributedRateLimiter NEVER = (permits) -> CompletableFuture.completedFuture(false);

  public static DistributedRateLimiter treefill(
      TreeFillConfig treeFillConfig,
      Ticker ticker,
      Executor executor,
      MessageDeliverator messageDeliverator,
      Random random
  ) {
    return new GenericNode(treeFillConfig.nodeId, treeFillConfig.clusterSize, treeFillConfig.permitsPerSecond, messageDeliverator);
  }

  public static DistributedRateLimiter divided(
      DividedConfig config,
      Ticker ticker
  ) {
    double localPermitsPerSecond = config.permitsPerSecond / (double) config.clusterSize;
    ForkedRateLimiter forkedRateLimiter = ForkedRateLimiter.create(localPermitsPerSecond, ticker);
    return (permits) -> CompletableFuture.completedFuture(forkedRateLimiter.tryAcquire((int) permits));
  }
}
