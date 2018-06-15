package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageSender;
import com.duprasville.limiters.testutil.TestTicker;

public class TreeFillRateLimiterTestBase {
  protected MockMessageSender mockMessageSender;
  protected TestTicker ticker;
  protected ExecutorService executor;

  public List<TreeFillRateLimiter> buildGraph(int N, int W) {
    return buildGraph(N, W, false);
  }

  public List<TreeFillRateLimiter> buildGraph(int N, int W, boolean useRandomizedWindowState) {
    TestTicker ticker = this.ticker;
    MockMessageSender mockMessageSender = this.mockMessageSender;

    List<TreeFillRateLimiter> nodes = new ArrayList<>();

    for (int i = 1; i <= N; i++) {
      TreeFillRateLimiter nodelet = new TreeFillRateLimiter(i, N, W, ticker, mockMessageSender, useRandomizedWindowState);
      mockMessageSender.addNode(nodelet);
      nodes.add(nodelet);
    }

    return nodes;
  }
}
