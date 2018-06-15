package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.ClusterRateLimiters;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageSender;
import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.treefill.TreeFillRateLimiter;
import com.duprasville.limiters.util.SerialExecutorService;
import com.google.common.base.Ticker;

public class DistributedRateLimiters {
  public static final DistributedRateLimiter UNLIMITED = (permits) -> CompletableFuture.completedFuture(true);
  public static final DistributedRateLimiter NEVER = (permits) -> CompletableFuture.completedFuture(false);

  public static DistributedRateLimiter fromClusterRateLimiter(
      ClusterRateLimiter clusterRateLimiter,
      ExecutorService executorService
  ) {
    return new DistributedRateLimiter() {
      private ClusterRateLimiter delegate = clusterRateLimiter;
      private ExecutorService executor = executorService;
      private SerialExecutorService serialExecutor = new SerialExecutorService(executor);


      @Override
      public CompletableFuture<Boolean> acquire() {
        return CompletableFuture.supplyAsync(() -> delegate.acquire(), executor);
      }

      @Override
      public CompletableFuture<Boolean> acquire(long permits) {
        return CompletableFuture.supplyAsync(() -> delegate.acquire(permits), executor);
      }

      @Override
      public CompletableFuture<Void> receive(Message message) {
        return CompletableFuture.runAsync(() -> delegate.receive(message), serialExecutor);
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
      FutureMessageSender futureMessageSender,
      ExecutorService executorService
      ) {

    MessageSender messageSender = FutureMessageSender.toMessageSender(futureMessageSender);

    TreeFillRateLimiter treeFillRateLimiter = new TreeFillRateLimiter(
        treeFillConfig.nodeId,
        treeFillConfig.clusterSize,
        treeFillConfig.permitsPerSecond,
        ticker,
        messageSender
    );

    return fromClusterRateLimiter(treeFillRateLimiter, executorService);
  }

  public static DistributedRateLimiter divided(
      long clusterSize,
      double permitsPerSecond,
      Ticker ticker,
      ExecutorService executorService
      ) {
    ClusterRateLimiter dividedRateLimiter = ClusterRateLimiters.divided(clusterSize, permitsPerSecond, ticker);

    return fromClusterRateLimiter(
        dividedRateLimiter,
        executorService
    );
  }
}
