package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.TestMessageSource;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class TreeFillRateLimiterTest {

    @Test
    void tryAcquireInt() {
        KaryTree karyTree = KaryTree.byMinCapacity(5L, 5L);
        TestMessageSource messageSource = new TestMessageSource();

        long nodeId = 3L;
        long clusterSize = karyTree.getCapacity();
        long permitsPerSecond = 5L;

        TreeFillRateLimiter treefill = new TreeFillRateLimiter(permitsPerSecond, nodeId, clusterSize, karyTree, messageSource);

        AtomicBoolean onSendCalled = new AtomicBoolean(false);
        messageSource.onSend((m) -> onSendCalled.set(true));

        boolean acquired = treefill.tryAcquire(2L);
        assertThat(acquired, is(true));
        assertThat(onSendCalled.get(), is(true));
    }

    @Test
    void sendsDetectWhenPermitsAcquired() {
        KaryTree karyTree = KaryTree.byMinCapacity(5L, 5L);
        TestTreeFillMessageSink messageSink = new TestTreeFillMessageSink();
        TestMessageSource messageSource = new TestMessageSource();
        messageSource.onSend(messageSink::receive);
        List<Detect> detectsReceived = Lists.newArrayList();
        messageSink.onDetect(detectsReceived::add);

        long nodeId = 3L;
        long clusterSize = karyTree.getCapacity(); // N
        long permitsPerSecond = 500L; // W

        TreeFillRateLimiter treefill = new TreeFillRateLimiter(permitsPerSecond, nodeId, clusterSize, karyTree, messageSource);
        boolean acquired = treefill.tryAcquire(2L);

        // rounds = log_K(W/N) where,
        //   K is the "ary" of the detect tree
        //   W is the number of events for the cluster to count (permits acquired)
        //   N is the number of nodes in the cluster
        // in this case, ceil(log_5(500.0/6.0)) == 3
        long permitsDetected = detectsReceived.stream().mapToLong(d -> d.permitsAcquired).sum();
        assertThat(permitsDetected, is(2L));
        assertThat(detectsReceived.size(), is(3));
    }
}