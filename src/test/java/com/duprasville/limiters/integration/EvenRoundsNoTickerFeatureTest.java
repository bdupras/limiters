package com.duprasville.limiters.integration;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.DistributedRateLimiters;
import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.integration.proxies.ProxyMessageDeliverator;
import com.duprasville.limiters.testutil.TestTicker;
import com.google.common.base.Ticker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EvenRoundsNoTickerFeatureTest {

  private DistributedRateLimiter treeNode1;
  private DistributedRateLimiter treeNode2;
  private DistributedRateLimiter treeNode3;
  private Ticker ticker;
  private ProxyMessageDeliverator deliverator;
  private Executor executor;
  private Random random;

  //Rate / N nodes = 8 per node TOTAL
  //Round 1 - 4 permits
  //Round 2 - 2 permits
  //Round 3 - 1 permit
  //Round 4 - 1 permit
  private int rate = 24;

  @BeforeEach
  void init() {
    this.ticker = new TestTicker(0L);
    this.executor = Executors.newSingleThreadExecutor();
    this.random = new Random(0xDEADBEEF);
    this.deliverator = new ProxyMessageDeliverator();

    treeNode1 = DistributedRateLimiters.treefill(new TreeFillConfig(1, 3, rate), ticker, executor, deliverator, random);
    treeNode2 = DistributedRateLimiters.treefill(new TreeFillConfig(2, 3, rate), ticker, executor, deliverator, random);
    treeNode3 = DistributedRateLimiters.treefill(new TreeFillConfig(3, 3, rate), ticker, executor, deliverator, random);

    deliverator.setNode(1, treeNode1);
    deliverator.setNode(2, treeNode2);
    deliverator.setNode(3, treeNode3);
  }

  //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start test)
  @Test
  void testStraightupBasicAcquire() throws ExecutionException, InterruptedException {
    //Round 1
    deliverator.acquireOrFailSynchronous(1, 4);
    deliverator.acquireOrFailSynchronous(2, 4);
    deliverator.acquireOrFailSynchronous(3, 4);

    //Round 2
    deliverator.acquireOrFailSynchronous(1, 2);
    deliverator.acquireOrFailSynchronous(2, 2);
    deliverator.acquireOrFailSynchronous(3, 2);

    //Round 3
    deliverator.acquireOrFailSynchronous(1, 1);
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(3, 1);

    //Round 4
    deliverator.acquireOrFailSynchronous(1, 1);
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(3, 1);

    //assert EVERY node is now rate limited
    Assertions.assertFalse(deliverator.acquireSingle(1), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(2), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(3), "Should have failed to acquire but actually acquired");
  }
  
  //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start test)
  @Test
  void testAllFromOneNode() throws ExecutionException, InterruptedException {
    //Round 1
    deliverator.acquireOrFailSynchronous(2, 4);
    deliverator.acquireOrFailSynchronous(2, 4);
    deliverator.acquireOrFailSynchronous(2, 4);

    //Round 2
    deliverator.acquireOrFailSynchronous(2, 2);
    deliverator.acquireOrFailSynchronous(2, 2);
    deliverator.acquireOrFailSynchronous(2, 2);

    //Round 3
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(2, 1);

    //Round 4
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(2, 1);
    deliverator.acquireOrFailSynchronous(2, 1);

    //assert EVERY node is now rate limited
    Assertions.assertFalse(deliverator.acquireSingle(1), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(2), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(deliverator.acquireSingle(3), "Should have failed to acquire but actually acquired");
  }

  //NO time advancement here, and exhaust nodes not so perfectly with message delays
  @Test
  void testWithMessageDelays() throws ExecutionException, InterruptedException {
    //TBD
  }
}