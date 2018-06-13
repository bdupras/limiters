package com.duprasville.limiters.integration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.DistributedRateLimiters;
import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.integration.proxies.DelayedProxyMsgDeliverator;
import com.duprasville.limiters.testutil.TestTicker;
import com.google.common.base.Ticker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UnevenDelayMessagesNoTickerFeatureTest {

  private DistributedRateLimiter treeNode1;
  private DistributedRateLimiter treeNode2;
  private DistributedRateLimiter treeNode3;
  private Ticker ticker;
  private DelayedProxyMsgDeliverator deliverator;
  private Executor executor;

  //Rate / N nodes = 8 per node TOTAL
  //Round 1 - 4 permits * 3 = 12
  //Round 2 - 2 permits * 3 = 6
  //Round 3 - 1 permit * 3 = 3
  //Round 4 - 1 permit * 3 = 3
  private int rate = 24;

  @BeforeEach
  void init() {
    this.ticker = new TestTicker(0L);
    this.executor = Executors.newSingleThreadExecutor();
    this.deliverator = new DelayedProxyMsgDeliverator();

    treeNode1 = DistributedRateLimiters.treefill(new TreeFillConfig(1, 3, rate), ticker, executor, deliverator);
    treeNode2 = DistributedRateLimiters.treefill(new TreeFillConfig(2, 3, rate), ticker, executor, deliverator);
    treeNode3 = DistributedRateLimiters.treefill(new TreeFillConfig(3, 3, rate), ticker, executor, deliverator);

    deliverator.setNode(1, treeNode1);
    deliverator.setNode(2, treeNode2);
    deliverator.setNode(3, treeNode3);
  }

  //NO time advancement here, and exhaust nodes not so perfectly with message delays
  @Test
  void testWithMessageDelaysEvenRounds() throws ExecutionException, InterruptedException {
    //Round 1
    deliverator.acquireOrFailSynchronous(1, 4);
    deliverator.acquireOrFailSynchronous(2, 4);
    deliverator.acquireOrFailSynchronous(3, 4);
    deliverator.releaseMessages();

    //Round 2
    deliverator.acquireOrFailSynchronous(1, 2);
    deliverator.acquireOrFailSynchronous(2, 2);
    deliverator.acquireOrFailSynchronous(3, 2);
    deliverator.releaseMessages();

    //Round 3
    deliverator.acquireOrFailSynchronous(1, 1);
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(3, 1);
    deliverator.releaseMessages();

    //Round 4 OVERSUBSCRIBED which is fine
    deliverator.acquireOrFailSynchronous(1, 2);
    deliverator.acquireOrFailSynchronous(2, 2);
    deliverator.acquireOrFailSynchronous(3, 2);
    deliverator.releaseMessages();

    //assert EVERY node is now rate limited
    Assertions.assertFalse(deliverator.acquireSingle(1), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(2), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(3), "Should have failed to acquire but actually acquired");
  }

  //NO time advancement here, and exhaust nodes not so perfectly with message delays
  @Test
  void testSomeUnevenOversubscribedRounds() throws ExecutionException, InterruptedException {
    //Round 1 - total 20 leading to Round 3!!!  round 1 total 12, then 6
    deliverator.acquireOrFailSynchronous(1, 6);
    deliverator.acquireOrFailSynchronous(2, 7);
    deliverator.acquireOrFailSynchronous(3, 7);
    deliverator.releaseMessages();

    //Round 2 virtually skipped

    //Round 3
    deliverator.acquireOrFailSynchronous(1, 3);
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(3, 1);
    deliverator.releaseMessages();

    //Round 4 virtually skipped

    //assert EVERY node is now rate limited
    Assertions.assertFalse(deliverator.acquireSingle(1), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(2), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(3), "Should have failed to acquire but actually acquired");
  }

  @Test
  void testOverLoadBottomNodes() throws ExecutionException, InterruptedException {
    //Round 1 - total 20 leading to Round 3!!!  round 1 total 12, then 6
    deliverator.acquireOrFailSynchronous(2, 6);
    deliverator.acquireOrFailSynchronous(2, 7);
    deliverator.acquireOrFailSynchronous(3, 7);
    deliverator.releaseMessages();

    //Round 2 virtually skipped

    //Round 3
    deliverator.acquireOrFailSynchronous(3, 3);
    deliverator.acquireOrFailSynchronous(3, 1);
    deliverator.acquireOrFailSynchronous(3, 1);
    deliverator.releaseMessages();

    //Round 4 virtually skipped

    //assert EVERY node is now rate limited
    Assertions.assertFalse(deliverator.acquireSingle(1), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(2), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(3), "Should have failed to acquire but actually acquired");
  }
}
