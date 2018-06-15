package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.duprasville.limiters.testutil.SameThreadExecutorService;
import com.duprasville.limiters.testutil.TestTicker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeFillRateLimiterExperiments extends TreeFillRateLimiterTestBase {

  private int rowLimit;

  @BeforeEach
  void init() {
    this.mockMessageSender = new MockMessageSender();
    this.ticker = new TestTicker(0L);
    this.executor = new SameThreadExecutorService();
  }

  @Test
  void runWithAndWithoutRandomization() throws ExecutionException, InterruptedException {
    rowLimit = 11;
    List<Integer> nonRandomizedResults = testManyTrees(false);
    List<Integer> randomizedResults = testManyTrees(true);

    System.out.println("Deterministic WindowStates results:");
    for (int i = 0; i < nonRandomizedResults.size(); i++) {
      int rows = i+1;
      int numLimiters = (int) (Math.pow(2, rows) - 1);
      System.out.println("\tFor rows " + rows + " and nodes " + numLimiters + " communication load: " + nonRandomizedResults.get(i));
    }

    Thread.sleep(1000);

    System.out.println("Non-deterministic WindowStates results:");
    for (int i = 0; i < randomizedResults.size(); i++) {
      int rows = i+1;
      int numLimiters = (int) (Math.pow(2, rows) - 1);
      System.out.println("\tFor rows " + rows + " and nodes " + numLimiters + " communication load: " + randomizedResults.get(i));
    }
  }

  List<Integer> testManyTrees(boolean useRandomizedWindowStates) throws ExecutionException, InterruptedException {
    List<Integer> communicationResults = new ArrayList<>(rowLimit);
    int numLimiters = 0;
    for (int rows = 1; rows <= rowLimit; rows++) {
      this.mockMessageSender = new MockMessageSender();

      numLimiters = (int) (Math.pow(2, rows) - 1);
      System.out.println("For rows " + rows + " and nodes " + numLimiters);

      List<TreeFillRateLimiter> nodes = buildGraph(numLimiters, numLimiters, useRandomizedWindowStates);
      TreeFillRateLimiter node = nodes.get(numLimiters - 1);
      assertTrue(node.acquire(numLimiters));

      assertFalse(node.currentWindow().windowOpen);

      System.out.println("Communication load: " + mockMessageSender.messageSentBetweenRateLimiters);
      communicationResults.add(mockMessageSender.messageSentBetweenRateLimiters);
    }
    return communicationResults;
  }

}
