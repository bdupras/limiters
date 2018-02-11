package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.TestMessageSource;
import com.duprasville.limiters.util.karytree.KaryTree;
import com.google.common.collect.Lists;
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
    private KaryTree karytree = KaryTree.byHeight(5L, 5L);
    private long PERMITS_PER_SECOND = 5000L;
    private long CLUSTER_SIZE = karytree.getCapacity() - 1L;
    private long ROUNDS = TreeFillMath.rounds(PERMITS_PER_SECOND, CLUSTER_SIZE);
    private TestMessageSource messageSource;
    private List<Detect> detectsEmitted;
    private List<Full> fullsEmitted;
    private TreeFillClusterRateLimiter rootNode;
    private TreeFillClusterRateLimiter innerNode;
    private TreeFillClusterRateLimiter baseNode;


    @BeforeEach
    void beforeEach() {
        TestTreeFillMessageSink messageSink = new TestTreeFillMessageSink();
        messageSource = new TestMessageSource();
        messageSource.onSend(messageSink::receive);
        detectsEmitted = Lists.newArrayList();
        fullsEmitted = Lists.newArrayList();
        messageSink.onDetect(detectsEmitted::add);
        messageSink.onFull(fullsEmitted::add);
        baseNode =
                new TreeFillClusterRateLimiter(
                        PERMITS_PER_SECOND,
                        CLUSTER_SIZE - 1,
                        karytree.getCapacity(),
                        karytree,
                        messageSource);
        innerNode =
                new TreeFillClusterRateLimiter(
                        PERMITS_PER_SECOND,
                        1L,
                        karytree.getCapacity(),
                        karytree,
                        messageSource);
        rootNode =
                new TreeFillClusterRateLimiter(
                        PERMITS_PER_SECOND,
                        0l,
                        karytree.getCapacity(),
                        karytree,
                        messageSource);
    }

    @Test
    void singleSingleNodeTryAcquireOnePermitRandom() {
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

    @Test
    void innerNodeSendsOneFullPerRoundAfterReceivingFullsFromAllChildren() {
        long[] children = innerNode.getNodeConfig().children;
        long expectedPermitsFilled = 0L;
        for (int round = 1; round <= ROUNDS; round++) {
            for (long child : children) {
                long permitsFilled = innerNode.getWindowConfig().nodePermitsPerRound[round] * 2;
                expectedPermitsFilled += permitsFilled;
                assertThat(fullsEmitted, hasSize(round - 1));
                innerNode.receive(new Full(
                        child,
                        innerNode.nodeId,
                        round,
                        permitsFilled));
            }
            assertThat(fullsEmitted, hasSize(round));

            // send one more than expected
            innerNode.receive(new Full(
                    children[0],
                    innerNode.nodeId,
                    round,
                    innerNode.getWindowConfig().nodePermitsPerRound[round]));
            assertThat(fullsEmitted, hasSize(round));
        }

        assertThat(
                fullsEmitted.stream().mapToLong(full -> full.permitsFilled).sum(),
                is(equalTo(expectedPermitsFilled))
        );

    }

    @Test
    void rootNodeBlort() {
        rootNode.receive(new Full(1L, 0L, 1L, PERMITS_PER_SECOND));
    }
}