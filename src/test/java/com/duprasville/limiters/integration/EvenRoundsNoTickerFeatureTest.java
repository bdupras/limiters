package com.duprasville.limiters.integration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.futureapi.DistributedRateLimiter;
import com.duprasville.limiters.futureapi.DistributedRateLimiters;
import com.duprasville.limiters.integration.proxies.ProxyMessageSender;
import com.duprasville.limiters.testutil.SameThreadExecutorService;
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
  private ProxyMessageSender messageSender;
  private ExecutorService executorService;

  //Rate / N nodes = 8 per node TOTAL
  //Round 1 - 4 permits
  //Round 2 - 2 permits
  //Round 3 - 1 permit
  //Round 4 - 1 permit
  private int rate = 24;

  @BeforeEach
  void init() {
    this.ticker = new TestTicker(0L);
    this.executorService = new SameThreadExecutorService();
    this.messageSender = new ProxyMessageSender();

    treeNode1 = DistributedRateLimiters.treefill(new TreeFillConfig(1, 3, rate), ticker, messageSender, executorService);
    treeNode2 = DistributedRateLimiters.treefill(new TreeFillConfig(2, 3, rate), ticker, messageSender, executorService);
    treeNode3 = DistributedRateLimiters.treefill(new TreeFillConfig(3, 3, rate), ticker, messageSender, executorService);

    messageSender.setNode(1, treeNode1);
    messageSender.setNode(2, treeNode2);
    messageSender.setNode(3, treeNode3);
  }

  //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start test)
  @Test
  void testStraightupBasicAcquire() throws ExecutionException, InterruptedException {
    //Round 1
    messageSender.acquireOrFailSynchronous(1, 4);
    messageSender.acquireOrFailSynchronous(2, 4);
    messageSender.acquireOrFailSynchronous(3, 4);

    //Round 2
    messageSender.acquireOrFailSynchronous(1, 2);
    messageSender.acquireOrFailSynchronous(2, 2);
    messageSender.acquireOrFailSynchronous(3, 2);

    //Round 3
    messageSender.acquireOrFailSynchronous(1, 1);
    messageSender.acquireOrFailSynchronous(2, 1);
    messageSender.acquireOrFailSynchronous(3, 1);

    //Round 4
    messageSender.acquireOrFailSynchronous(1, 1);
    messageSender.acquireOrFailSynchronous(2, 1);
    messageSender.acquireOrFailSynchronous(3, 1);

    //assert EVERY node is now rate limited
    Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but actually acquired");
  }

  //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start test)
  @Test
  void testAllFromOneNode() throws ExecutionException, InterruptedException {
    //Round 1
    messageSender.acquireOrFailSynchronous(2, 4);
    messageSender.acquireOrFailSynchronous(2, 4);
    messageSender.acquireOrFailSynchronous(2, 4);

    //Round 2
    messageSender.acquireOrFailSynchronous(2, 2);
    messageSender.acquireOrFailSynchronous(2, 2);
    messageSender.acquireOrFailSynchronous(2, 2);

    //Round 3
    messageSender.acquireOrFailSynchronous(2, 1);
    messageSender.acquireOrFailSynchronous(2, 1);
    messageSender.acquireOrFailSynchronous(2, 1);

    //Round 4
    messageSender.acquireOrFailSynchronous(2, 1);
    messageSender.acquireOrFailSynchronous(2, 1);
    messageSender.acquireOrFailSynchronous(2, 1);

    //assert EVERY node is now rate limited
    Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but actually acquired");
    Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but actually acquired");
  }

}
