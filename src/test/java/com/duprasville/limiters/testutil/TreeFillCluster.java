package com.duprasville.limiters.testutil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.duprasville.limiters.futureapi.DistributedRateLimiter;
import com.duprasville.limiters.futureapi.DistributedRateLimiters;
import com.duprasville.limiters.treefill.TreeFillRateLimiter;

public class TreeFillCluster {
    final Map<Long, DistributedRateLimiter> nodes;
    private final long permitsPerSecond;
    private final TestTicker ticker;
    private final Executor executor;
    final TestMessageDeliverator testMessageDeliverator;
    final long clusterSize;
    final Random random;

    public TreeFillCluster(long N, long W, TestTicker ticker, Executor executor, Random random) {
        this.clusterSize = N;
        this.permitsPerSecond = W;
        this.ticker = ticker;
        this.executor = executor;
        this.random = random;

        this.nodes = new HashMap<>((int)N);
        this.testMessageDeliverator = new TestMessageDeliverator(); //TODO record all test messages
        this.testMessageDeliverator.onSend((message) -> nodes.get(message.getDst()).receive(message));

        for (long m = 0; m < N; m++) {
            TreeFillRateLimiter node = new TreeFillRateLimiter(
                    m,
                    N,
                    W,
                    ticker,
                    testMessageDeliverator
            );
            this.nodes.put(m, DistributedRateLimiters.fromClusterRateLimiter(node));
        }
    }

    public CompletableFuture<Boolean> acquire(long permits) {
        long m = random.nextInt((int) clusterSize);
        return nodes.get(m).acquire(permits);
    }
}
