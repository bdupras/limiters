package com.duprasville.limiters.treefill;

import com.duprasville.limiters.testutil.SameThreadExecutorService;
import com.duprasville.limiters.testutil.TestTicker;
import com.duprasville.limiters.testutil.TreeFillCluster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BriansTreeFillRateLimiterTest {
    private Random random = new Random(0xDEADBEEF);
    private long N = 3;
    private long W = N * (long)Math.pow(2, 2);
    private long ROUNDS = TreeFillMath.rounds(W, N);
    private Executor executor = new SameThreadExecutorService();

    TreeFillCluster cluster;
    private TestTicker ticker = new TestTicker(0L);

    @BeforeEach
    void setup() {
        cluster = new TreeFillCluster(N, W, ticker, executor, random);
    }

    @Test
    void acquire() throws Exception {
        assertTrue(cluster.acquire(1L).get());
    }
}
