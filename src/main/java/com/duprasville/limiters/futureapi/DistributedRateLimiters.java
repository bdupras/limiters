package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.ClusterRateLimiters;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.treefill.TreeFillRateLimiter;
import com.google.common.base.Ticker;

import static com.duprasville.limiters.futureapi.FutureMessageDeliverator.toMessageDeliverator;

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
      FutureMessageDeliverator futureMessageDeliverator
  ) {
    MessageDeliverator messageDeliverator = toMessageDeliverator(futureMessageDeliverator);
    return fromClusterRateLimiter(
        new TreeFillRateLimiter(
            treeFillConfig.nodeId,
            treeFillConfig.clusterSize,
            treeFillConfig.permitsPerSecond,
            ticker,
            messageDeliverator
        )
    );
  }

  public static DistributedRateLimiter divided(long clusterSize, double permitsPerSecond, Ticker ticker) {
    return fromClusterRateLimiter(ClusterRateLimiters.divided(clusterSize, permitsPerSecond, ticker));
  }

}
