package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.*;
import com.duprasville.limiters.util.karytree.KaryTree;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class TreeFillRateLimiterTest {

    @Test
    void tryAcquireInt() {
        KaryTree karyTree = KaryTree.byMinCapacity(5L, 5L);
        TestMessageSender messageSender = new TestMessageSender();

        long nodeId = 3L;
        long clusterSize = karyTree.getCapacity();
        long permitsPerSecond = 5L;

        TreeFillRateLimiter treefill = new TreeFillRateLimiter(permitsPerSecond, nodeId, clusterSize, karyTree, messageSender);

        AtomicBoolean onSendCalled = new AtomicBoolean(false);
        messageSender.onSend((s, d, m) -> {
            onSendCalled.set(true);
            messageSender.receive(s, d, m);
        });

        boolean acquired = treefill.tryAcquire(2L);
        assertThat(acquired, is(true));
        assertThat(onSendCalled.get(), is(true));
    }
}