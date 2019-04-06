package com.duprasville.limiters.integration;

import com.duprasville.limiters.api.TreeFillConfig;
import com.duprasville.limiters.futureapi.DistributedRateLimiter;
import com.duprasville.limiters.futureapi.DistributedRateLimiters;
import com.duprasville.limiters.integration.proxies.DelayedProxyMessageSender;
import com.duprasville.limiters.testutil.SameThreadExecutorService;
import com.google.common.base.Ticker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

public class TimeAdvancementAndRandomizedMessageSendingFeatureTest {
    private DistributedRateLimiter treeNode1;
    private DistributedRateLimiter treeNode2;
    private DistributedRateLimiter treeNode3;
    private Ticker ticker;
    private DelayedProxyMessageSender messageSender;
    private ExecutorService executorService;

    //Rate / N nodes = 8 per node TOTAL
    //Round 1 - 4 permits * 3 = 12
    //Round 2 - 2 permits * 3 = 6
    //Round 3 - 1 permit * 3 = 3
    //Round 4 - 1 permit * 3 = 3
    private int rate = 24;

    @BeforeEach
    void init() {
        this.ticker = Ticker.systemTicker();
        this.executorService = new SameThreadExecutorService();
        this.messageSender = new DelayedProxyMessageSender();

        treeNode1 = DistributedRateLimiters.randomizedTreefill(new TreeFillConfig(1, 3, rate),
            ticker, messageSender, executorService);
        treeNode2 = DistributedRateLimiters.randomizedTreefill(new TreeFillConfig(2, 3, rate),
            ticker, messageSender, executorService);
        treeNode3 = DistributedRateLimiters.randomizedTreefill(new TreeFillConfig(3, 3, rate),
            ticker, messageSender, executorService);

        messageSender.setNode(1, treeNode1);
        messageSender.setNode(2, treeNode2);
        messageSender.setNode(3, treeNode3);
    }

    @Test
    void testWithMessageDelaysEvenRounds() {
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

    @Test
    void testSomeUnevenOversubscribedRounds() {
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
    void testOverLoadBottomNodes() {
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
        Assertions.assertFalse(messageSender.acquireSingle(1), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(2), "Should have failed to acquire but" +
            " actually acquired");
        Assertions.assertFalse(messageSender.acquireSingle(3), "Should have failed to acquire but" +
            " actually acquired");
    }
}
