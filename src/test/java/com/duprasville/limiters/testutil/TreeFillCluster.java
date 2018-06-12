package com.duprasville.limiters.testutil;

import com.duprasville.limiters.treefill.TreeFillRateLimiter;
import com.duprasville.limiters.util.KaryTree;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;

public class TreeFillCluster {
    final Map<Long, TreeFillRateLimiter> nodes;
    private final long permitsPerSecond;
    private final TestTicker ticker;
    private final Executor executor;
    final TestMessageSender messageSender;
    final TestMessageReceiver messageReceiver;
    final long clusterSize;
    final Random random;

    public TreeFillCluster(KaryTree karytree, long clusterSize, long permitsPerSecond, TestTicker ticker, Executor executor, Random random) {
        this.clusterSize = clusterSize;
        this.permitsPerSecond = permitsPerSecond;
        this.ticker = ticker;
        this.executor = executor;
        this.random = random;
        this.messageReceiver = new TestMessageReceiver(); //TODO record all test messages

        this.nodes = new HashMap<>((int)clusterSize);
        this.messageSender = new TestMessageSender(random);
        this.messageSender.onSend((message) -> {
            nodes.get(message.getDst()).receive(message);
            messageReceiver.receive(message);
        });

        for (long m = 0; m < clusterSize; m++) {
            TreeFillRateLimiter node = new TreeFillRateLimiter(
                    karytree,
                    m,
                    clusterSize,
                    permitsPerSecond,
                    ticker,
                    executor,
                    messageSender,
                    random

            );
            this.nodes.put(m, node);
        }
    }

    public boolean tryAcquire(long permits) {
        long m = random.nextInt((int) clusterSize);
        return nodes.get(m).tryAcquire(permits);
    }
}
