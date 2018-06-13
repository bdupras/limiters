package com.duprasville.limiters.integration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.NodeConfig;
import com.duprasville.limiters.api.TreeFillNodeFactory;
import com.duprasville.limiters.integration.proxies.ProxyMessageDeliverator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FirstIntegFeatureTest {

  private ProxyMessageDeliverator deliverator;
  private Executor executor = Executors.newSingleThreadExecutor();
  private DistributedRateLimiter treeNode1;
  private DistributedRateLimiter treeNode2;
  private DistributedRateLimiter treeNode3;

  //Rate / N nodes = 8 per node TOTAL
  //Round 1 - 4 permits
  //Round 2 - 2 permits
  //Round 3 - 1 permit
  //Round 4 - 1 permit
  private int rate = 24;

  @BeforeEach
  void init() {
    deliverator = new ProxyMessageDeliverator();
    treeNode1 = TreeFillNodeFactory.createNode(deliverator, executor, new NodeConfig(3, 1));
    treeNode2 = TreeFillNodeFactory.createNode(deliverator, executor, new NodeConfig(3, 2));
    treeNode3 = TreeFillNodeFactory.createNode(deliverator, executor, new NodeConfig(3, 3));

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

}
