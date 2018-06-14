package com.duprasville.limiters.api;

import com.duprasville.limiters.treefill.TreeFillRateLimiter;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ForkedRateLimiter;

public class ClusterRateLimiters {
  public static final ClusterRateLimiter UNLIMITED = (permits) -> true;
  public static final ClusterRateLimiter NEVER = (permits) -> false;

  public static ClusterRateLimiter treefill(
      TreeFillConfig treeFillConfig,
      Ticker ticker,
      MessageDeliverator messageDeliverator
  ) {
    return
        new TreeFillRateLimiter(treeFillConfig.nodeId, treeFillConfig.clusterSize, treeFillConfig.permitsPerSecond, ticker, messageDeliverator);
  }

  public static ClusterRateLimiter divided(long clusterSize, double permitsPerSecond, Ticker ticker) {
    double localPermitsPerSecond = permitsPerSecond / (double) clusterSize;
    final ForkedRateLimiter forkedRateLimiter = ForkedRateLimiter.create(localPermitsPerSecond, ticker);

    return new ClusterRateLimiter() {
      @Override
      public boolean acquire(long permits) {
        return forkedRateLimiter.tryAcquire((int) permits);
      }

      @Override
      public void setRate(long permitsPerSecond) {
        forkedRateLimiter.setRate(permitsPerSecond);
      }
    };
  }
}
