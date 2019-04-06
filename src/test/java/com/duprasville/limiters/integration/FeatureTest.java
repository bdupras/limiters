package com.duprasville.limiters.integration;

import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.futureapi.DistributedRateLimiter;
import com.duprasville.limiters.futureapi.DistributedRateLimiters;
import com.duprasville.limiters.futureapi.FutureMessageSender;
import com.duprasville.limiters.integration.proxies.DelayedProxyMessageSender;
import com.duprasville.limiters.integration.proxies.ProxyMessageSender;
import com.duprasville.limiters.testutil.SameThreadExecutorService;
import com.duprasville.limiters.testutil.TestTicker;
import com.google.common.base.Ticker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

class FeatureTest {

    private DistributedRateLimiter treeNode1;
    private DistributedRateLimiter treeNode2;
    private DistributedRateLimiter treeNode3;
    private Ticker ticker;
    private ExecutorService executorService;

    //Rate / N nodes = 8 per node TOTAL
    //Round 1 - 4 permits
    //Round 2 - 2 permits
    //Round 3 - 1 permit
    //Round 4 - 1 permit
    private int rate = 24;

    // this is the most basic version of the algorithm we came up with.
    private void initNoTimeAdvancement() {
        this.ticker = new TestTicker(0L);
        this.executorService = new SameThreadExecutorService();
    }

    // this is with multiple windows over time
    private void initWithTimeElapsing() {
        this.ticker = Ticker.systemTicker();
        this.executorService = new SameThreadExecutorService();
    }

    private void buildRegularTree(FutureMessageSender messageSender) {
        treeNode1 = DistributedRateLimiters.treefill(new TreeFillConfig(1, 3, rate), ticker,
            messageSender, executorService);
        treeNode2 = DistributedRateLimiters.treefill(new TreeFillConfig(2, 3, rate), ticker,
            messageSender, executorService);
        treeNode3 = DistributedRateLimiters.treefill(new TreeFillConfig(3, 3, rate), ticker,
            messageSender, executorService);
    }

    private void buildRandomizedTree(FutureMessageSender messageSender) {
        treeNode1 = DistributedRateLimiters.randomizedTreefill(new TreeFillConfig(1, 3, rate),
            ticker, messageSender, executorService);
        treeNode2 = DistributedRateLimiters.randomizedTreefill(new TreeFillConfig(2, 3, rate),
            ticker, messageSender, executorService);
        treeNode3 = DistributedRateLimiters.randomizedTreefill(new TreeFillConfig(3, 3, rate),
            ticker, messageSender, executorService);
    }

    //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start
    // test)
    @Test
    void testStraightupBasicAcquireNoRandomization() {
        ProxyMessageSender messageSender = new ProxyMessageSender();

        initNoTimeAdvancement();

        buildRegularTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

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
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but" +
            " actually acquired");
    }

    //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start
    // test)
    @Test
    void testAllFromOneNodeNoRandomization() {
        ProxyMessageSender messageSender = new ProxyMessageSender();

        initNoTimeAdvancement();

        buildRegularTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

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
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but" +
            " actually acquired");
    }

    // while our solution to the problem is different from Kim's paper,
    // this is one step further down the path of 'we tried it and reality was different
    // than academically specified'.
    // 'Randomization' refers to how messages are sent to other nodes in the tree.

    //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start
    // test)
    @Test
    void testStraightupBasicAcquireWithRandomization() {
        ProxyMessageSender messageSender = new ProxyMessageSender();

        initNoTimeAdvancement();

        buildRandomizedTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

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
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but" +
            " actually acquired");
    }

    //NO time advancement here, and exhaust nodes perfectly (not realistic but a very basic start
    // test)
    @Test
    void testAllFromOneNodeWithRandomization() {
        ProxyMessageSender messageSender = new ProxyMessageSender();

        initNoTimeAdvancement();

        buildRandomizedTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

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
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but" +
            " actually acquired");
    }


    // now we're getting into fancier edge cases - what happens when we have uneven delay?

    //NO time advancement here, and exhaust nodes not so perfectly with message delays
    @Test
    void testWithMessageDelaysEvenRoundsAndNoRandomization() {
        DelayedProxyMessageSender messageSender = new DelayedProxyMessageSender();

        initNoTimeAdvancement();

        buildRegularTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

        //Round 1
        messageSender.acquireOrFailSynchronous(1, 4);
        messageSender.acquireOrFailSynchronous(2, 4);
        messageSender.acquireOrFailSynchronous(3, 4);
        messageSender.releaseMessages();

        //Round 2
        messageSender.acquireOrFailSynchronous(1, 2);
        messageSender.acquireOrFailSynchronous(2, 2);
        messageSender.acquireOrFailSynchronous(3, 2);
        messageSender.releaseMessages();

        //Round 3
        messageSender.acquireOrFailSynchronous(1, 1);
        messageSender.acquireOrFailSynchronous(2, 1);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.releaseMessages();

        //Round 4 OVERSUBSCRIBED which is fine
        messageSender.acquireOrFailSynchronous(1, 2);
        messageSender.acquireOrFailSynchronous(2, 2);
        messageSender.acquireOrFailSynchronous(3, 2);
        messageSender.releaseMessages();

        //assert EVERY node is now rate limited
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but" +
            " actually acquired");
    }

    //NO time advancement here, and exhaust nodes not so perfectly with message delays
    @Test
    void testSomeUnevenOversubscribedRoundsAndNoRandomization() {
        DelayedProxyMessageSender messageSender = new DelayedProxyMessageSender();

        initNoTimeAdvancement();

        buildRegularTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

        //Round 1 - total 20 leading to Round 3!!!  round 1 total 12, then 6
        messageSender.acquireOrFailSynchronous(1, 6);
        messageSender.acquireOrFailSynchronous(2, 7);
        messageSender.acquireOrFailSynchronous(3, 7);
        messageSender.releaseMessages();

        //Round 2 virtually skipped

        //Round 3
        messageSender.acquireOrFailSynchronous(1, 3);
        messageSender.acquireOrFailSynchronous(2, 1);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.releaseMessages();

        //Round 4 virtually skipped

        //assert EVERY node is now rate limited
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but" +
            " actually acquired");
    }

    @Test
    void testOverLoadBottomNodesAndNoRandomization() {
        DelayedProxyMessageSender messageSender = new DelayedProxyMessageSender();

        initNoTimeAdvancement();

        buildRegularTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

        //Round 1 - total 20 leading to Round 3!!!  round 1 total 12, then 6
        messageSender.acquireOrFailSynchronous(2, 6);
        messageSender.acquireOrFailSynchronous(2, 7);
        messageSender.acquireOrFailSynchronous(3, 7);
        messageSender.releaseMessages();

        //Round 2 virtually skipped

        //Round 3
        messageSender.acquireOrFailSynchronous(3, 3);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.releaseMessages();

        //Round 4 virtually skipped

        //assert EVERY node is now rate limited
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but actually acquired");
    }

    // uneven delays AND randomized message sending

    //NO time advancement here, and exhaust nodes not so perfectly with message delays
    @Test
    void testWithMessageDelaysEvenRoundsAndRandomization() {
        DelayedProxyMessageSender messageSender = new DelayedProxyMessageSender();

        initNoTimeAdvancement();

        buildRandomizedTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

        //Round 1
        messageSender.acquireOrFailSynchronous(1, 4);
        messageSender.acquireOrFailSynchronous(2, 4);
        messageSender.acquireOrFailSynchronous(3, 4);
        messageSender.releaseMessages();

        //Round 2
        messageSender.acquireOrFailSynchronous(1, 2);
        messageSender.acquireOrFailSynchronous(2, 2);
        messageSender.acquireOrFailSynchronous(3, 2);
        messageSender.releaseMessages();

        //Round 3
        messageSender.acquireOrFailSynchronous(1, 1);
        messageSender.acquireOrFailSynchronous(2, 1);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.releaseMessages();

        //Round 4 OVERSUBSCRIBED which is fine
        messageSender.acquireOrFailSynchronous(1, 2);
        messageSender.acquireOrFailSynchronous(2, 2);
        messageSender.acquireOrFailSynchronous(3, 2);
        messageSender.releaseMessages();

        //assert EVERY node is now rate limited
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but actually acquired");
    }

    //NO time advancement here, and exhaust nodes not so perfectly with message delays
    @Test
    void testSomeUnevenOversubscribedRoundsAndRandomization() {
        DelayedProxyMessageSender messageSender = new DelayedProxyMessageSender();

        initNoTimeAdvancement();

        buildRandomizedTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

        //Round 1 - total 20 leading to Round 3!!!  round 1 total 12, then 6
        messageSender.acquireOrFailSynchronous(1, 6);
        messageSender.acquireOrFailSynchronous(2, 7);
        messageSender.acquireOrFailSynchronous(3, 7);
        messageSender.releaseMessages();

        //Round 2 virtually skipped

        //Round 3
        messageSender.acquireOrFailSynchronous(1, 3);
        messageSender.acquireOrFailSynchronous(2, 1);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.releaseMessages();

        //Round 4 virtually skipped

        //assert EVERY node is now rate limited
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but actually acquired");
    }

    @Test
    void testOverLoadBottomNodesWithRandomization() {
        DelayedProxyMessageSender messageSender = new DelayedProxyMessageSender();

        initNoTimeAdvancement();

        buildRandomizedTree(messageSender);
        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);

        //Round 1 - total 20 leading to Round 3!!!  round 1 total 12, then 6
        messageSender.acquireOrFailSynchronous(2, 6);
        messageSender.acquireOrFailSynchronous(2, 7);
        messageSender.acquireOrFailSynchronous(3, 7);
        messageSender.releaseMessages();

        //Round 2 virtually skipped

        //Round 3
        messageSender.acquireOrFailSynchronous(3, 3);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.acquireOrFailSynchronous(3, 1);
        messageSender.releaseMessages();

        //Round 4 virtually skipped

        //assert EVERY node is now rate limited
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but actually acquired");
    }
}
