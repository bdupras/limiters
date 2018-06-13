package com.duprasville.limiters.integration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.NodeConfig;
import com.duprasville.limiters.api.TreeFillNodeFactory;
import com.duprasville.limiters.integration.proxies.ProxyMessageDeliverator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class FirstIntegFeatureTest {


  private ProxyMessageDeliverator deliverator;
  private Executor executor = Executors.newSingleThreadExecutor();
  private DistributedRateLimiter treeNode1;
  private DistributedRateLimiter treeNode2;
  private DistributedRateLimiter treeNode3;

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

  @Test
  void testAcquire() throws ExecutionException, InterruptedException {
  }

}
