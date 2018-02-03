package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.TestMessageSource;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.LongStream;

import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class TreeFillClusterRateLimiterTest {
    TestMessageSource messageSource;
    TestTreeFillMessageSink messageSink;
    List<Detect> detectsReceived;
    KaryTree karyTree_5x5_781 = KaryTree.byHeight(5L, 5L);

    @BeforeEach
    void beforeEach() {
        messageSink = new TestTreeFillMessageSink();
        messageSource = new TestMessageSource();
        messageSource.onSend(messageSink::receive);
        detectsReceived = Lists.newArrayList();
        messageSink.onDetect((d) -> {
            if (!detectsReceived.add(d)) {
                throw new RuntimeException("wtf");
            }
        } );
    }

    @Test
    void singleSingleNodeTryAcquireOnePermit() {
        for (int i = 0; i < 10; i++) {
            beforeEach();

            int seed = new Random().nextInt();
            Random rand = new Random(seed);

            long clusterSize = rand.nextInt(6_000) + 1L;
            long clusterNodeId = (long)rand.nextInt(toIntExact(clusterSize));
            long clusterPermits = rand.nextInt(250_000) + 1L;
            String genspec = format("[seed:%d W:%d n:%d N:%d] ", seed, clusterPermits, clusterNodeId, clusterSize);
            TreeFillClusterRateLimiter treefill = new TreeFillClusterRateLimiter(
                    clusterPermits,
                    clusterNodeId,
                    clusterSize,
                    karyTree_5x5_781,
                    messageSource
            );
            for (int j = 0; j < clusterPermits; j++) {
                treefill.tryAcquire(1L);
            }

            long permitsDetected = detectsReceived.stream().mapToLong(d -> d.permitsAcquired).sum();
            assertThat(genspec, permitsDetected, is(equalTo(clusterPermits)));
        }
    }

    @Test
    void sendsMultipleDetectsWhenMultiplePermitsAreAcquiredAtOnce() {
        TreeFillClusterRateLimiter treefill = new TreeFillClusterRateLimiter(
                5000L,
                3L,
                karyTree_5x5_781.getCapacity(),
                karyTree_5x5_781, messageSource
        );
        assertThat(treefill.tryAcquire(6L), is(true));
        assertThat(detectsReceived.size(), is(2));
        long permitsDetected = detectsReceived.stream().mapToLong(d -> d.permitsAcquired).sum();
        assertThat(permitsDetected, is(6L));
    }
}