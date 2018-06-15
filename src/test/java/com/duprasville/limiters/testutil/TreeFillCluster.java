package com.duprasville.limiters.testutil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.futureapi.DistributedRateLimiter;
import com.duprasville.limiters.futureapi.DistributedRateLimiters;

public class TreeFillCluster {
  final Map<Long, DistributedRateLimiter> nodes;
  private final long permitsPerSecond;
  private final TestTicker ticker;
  private final ExecutorService executorService;
  final TestFutureMessageSender testMessageSender;
  final long clusterSize;
  final Random random;

  public TreeFillCluster(long N, long W, TestTicker ticker, ExecutorService executorService, Random random) {
    this.clusterSize = N;
    this.permitsPerSecond = W;
    this.ticker = ticker;
    this.executorService = executorService;
    this.random = random;

    this.nodes = new HashMap<>((int) N);
    this.testMessageSender = new TestFutureMessageSender(); //TODO record all test messages
    this.testMessageSender.onSend((message) -> nodes.get(message.getDst()).receive(message));

    for (long m = 1; m <= N; m++) {
      nodes.put(m, DistributedRateLimiters.treefill(
          new TreeFillConfig(m, N, W),
          ticker,
          testMessageSender,
          executorService
      ));
    }
  }

  public CompletableFuture<Boolean> acquire() {
    return acquire(1L);
  }

  public CompletableFuture<Boolean> acquire(long permits) {
    long m = random.nextInt((int) clusterSize) + 1;
    return nodes.get(m).acquire(permits);
  }
}
