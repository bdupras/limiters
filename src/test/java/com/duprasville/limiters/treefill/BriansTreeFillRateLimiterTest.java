package com.duprasville.limiters.treefill;

import java.util.Random;
import java.util.concurrent.ExecutorService;

import com.duprasville.limiters.testutil.SameThreadExecutorService;
import com.duprasville.limiters.testutil.TestTicker;
import com.duprasville.limiters.testutil.TreeFillCluster;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BriansTreeFillRateLimiterTest {
  private Random random = new Random(0xDEADBEEF);
  private long N = 3;
  private long W = N * (long) Math.pow(2, 2);
  private long ROUNDS = TreeFillMath.rounds(W, N);
  private ExecutorService executorService = new SameThreadExecutorService();

  private TreeFillCluster cluster;
  private TestTicker ticker = new TestTicker(0L);

  @BeforeEach
  void setup() {
    cluster = new TreeFillCluster(N, W, ticker, executorService, random);
  }

  @Test
  void acquire() throws Exception {
    for (long i = 0; i < W; i++) {
      assertTrue(cluster.acquire().get());
    }
  }
}
