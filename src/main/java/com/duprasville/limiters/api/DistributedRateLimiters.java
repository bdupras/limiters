package com.duprasville.limiters.api;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.treefill.TreeFillRateLimiter;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ForkedRateLimiter;

public class DistributedRateLimiters {
  public static final DistributedRateLimiter UNLIMITED = (permits) -> CompletableFuture.completedFuture(true);
  public static final DistributedRateLimiter NEVER = (permits) -> CompletableFuture.completedFuture(false);

  public static DistributedRateLimiter fromClusterRateLimiter(ClusterRateLimiter clusterRateLimiter) {
    return new DistributedRateLimiter() {
      private ClusterRateLimiter delegate = clusterRateLimiter;

      @Override
      public CompletableFuture<Void> receive(Message message) {
        delegate.receive(message);
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletableFuture<Boolean> acquire(long permits) {
        return CompletableFuture.completedFuture(delegate.acquire(permits));
      }

      @Override
      public CompletableFuture<Boolean> acquire() {
        return CompletableFuture.completedFuture(delegate.acquire());
      }

      @Override
      public void setRate(long permitsPerSecond) {
        delegate.setRate(permitsPerSecond);
      }
    };
  }

  public static DistributedRateLimiter treefill(
      TreeFillConfig treeFillConfig,
      Ticker ticker,
      MessageDeliverator messageDeliverator
  ) {
    return fromClusterRateLimiter(
        new TreeFillRateLimiter(treeFillConfig.nodeId, treeFillConfig.clusterSize, treeFillConfig.permitsPerSecond, ticker, messageDeliverator)
    );
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
