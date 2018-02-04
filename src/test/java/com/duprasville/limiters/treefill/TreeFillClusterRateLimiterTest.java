package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.TestMessageSource;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.collect.Lists;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

class TreeFillClusterRateLimiterTest {
    TestMessageSource messageSource;
    TestTreeFillMessageSink messageSink;
    List<Detect> detectsEmitted;
    List<Full> fullsEmitted;
    KaryTree karytree = KaryTree.byHeight(5L, 5L);
    TreeFillClusterRateLimiter baseNode;

    @BeforeEach
    void beforeEach() {
        messageSink = new TestTreeFillMessageSink();
        messageSource = new TestMessageSource();
        messageSource.onSend(messageSink::receive);
        detectsEmitted = Lists.newArrayList();
        fullsEmitted = Lists.newArrayList();
        messageSink.onDetect(detectsEmitted::add);
        messageSink.onFull(fullsEmitted::add);
        baseNode =
                new TreeFillClusterRateLimiter(
                        5000L,
                        karytree.getCapacity()-1L,
                        karytree.getCapacity(),
                        karytree,
                        messageSource);
    }

    @Test
    void singleSingleNodeTryAcquireOnePermit() {
        for (int i = 0; i < 10; i++) {
            beforeEach();

            int seed = new Random().nextInt();
            Random rand = new Random(seed);

            long clusterSize = rand.nextInt(6_000) + 1L;
            long clusterNodeId = (long) rand.nextInt(toIntExact(clusterSize));
            long clusterPermits = rand.nextInt(250_000) + 1L;
            String genspec = format("[seed:%d W:%d n:%d N:%d] ", seed, clusterPermits, clusterNodeId, clusterSize);
            TreeFillClusterRateLimiter treefill = new TreeFillClusterRateLimiter(
                    clusterPermits,
                    clusterNodeId,
                    clusterSize,
                    karytree,
                    messageSource
            );
            for (int j = 0; j < clusterPermits; j++) {
                treefill.tryAcquire(1L);
            }

            long permitsDetected = detectsEmitted.stream().mapToLong(d -> d.permitsAcquired).sum();
            assertThat(genspec, permitsDetected, is(equalTo(clusterPermits)));
        }
    }

    @Test
    void sendsMultipleDetectsWhenMultiplePermitsAreAcquiredAtOnce() {
        TreeFillClusterRateLimiter treefill = new TreeFillClusterRateLimiter(
                5000L,
                3L,
                karytree.getCapacity(),
                karytree, messageSource
        );
        assertThat(treefill.tryAcquire(6L), is(true));
        assertThat(detectsEmitted.size(), is(2));
        long permitsDetected = detectsEmitted.stream().mapToLong(d -> d.permitsAcquired).sum();
        assertThat(permitsDetected, is(6L));
    }

    @Test
    void baseNodeFillsUpSendsFullAndForwardsDetect() {
        baseNode.receive(new Detect(
                1L,
                karytree.getCapacity() - 1L,
                1L,
                4L
        ));
        assertThat(detectsEmitted, is(empty()));
        assertThat(fullsEmitted, is(empty()));
        baseNode.receive(new Detect(
                1L,
                karytree.getCapacity() - 1L,
                1L,
                4L
        ));
        assertThat(detectsEmitted, is(empty()));
        assertThat(fullsEmitted, hasSize(1));
        baseNode.receive(new Detect(
                1L,
                karytree.getCapacity() - 1L,
                1L,
                4L
        ));
        assertThat(detectsEmitted, hasSize(1));
        assertThat(fullsEmitted, hasSize(1));
    }
}