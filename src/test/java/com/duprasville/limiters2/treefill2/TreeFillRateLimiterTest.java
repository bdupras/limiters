package com.duprasville.limiters2.treefill2;

import com.duprasville.limiters2.testutil.SameThreadExecutorService;
import com.duprasville.limiters2.testutil.TestTicker;
import com.duprasville.limiters2.testutil.TreeFillCluster;
import com.duprasville.limiters2.util.KaryTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeFillRateLimiterTest {
    private Random random = new Random(0xDEADBEEF);
    private KaryTree karytree = KaryTree.byHeight(5L, 5L);
    private long PERMITS_PER_SECOND = 5_000L;
    private long CLUSTER_SIZE = karytree.getCapacity() - 1L;
    private long ROUNDS = TreeFillMath.rounds(PERMITS_PER_SECOND, CLUSTER_SIZE);
    private Executor executor = new SameThreadExecutorService();

    TreeFillCluster cluster;
    private TestTicker ticker = new TestTicker(0L);

    @BeforeEach
    void setup() {
        cluster = new TreeFillCluster(karytree, CLUSTER_SIZE, PERMITS_PER_SECOND, ticker, executor, random);
    }

    @Test
    void tryAcquire() {
        assertTrue(cluster.tryAcquire(10));
        assertFalse(cluster.tryAcquire(100_000));
    }
}
