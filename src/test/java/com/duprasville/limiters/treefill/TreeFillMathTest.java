package com.duprasville.limiters.treefill;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.duprasville.limiters.treefill.TreeFillMath.clusterPermitsToDetectPerRound;
import static com.duprasville.limiters.treefill.TreeFillMath.nodePermitsToDetectPerRound;
import static java.lang.Math.min;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

class TreeFillMathTest {
    @Test
    void testClusterRemainingTriggersPerRound() {
        int seed = new Random().nextInt();
        Random rand = new Random(seed);

        for (int i = 0; i < 50; i++) {
            long clusterSize = rand.nextInt(6_000) + 1L;
            long clusterPermits = rand.nextInt(250_000) + 1L;
            long[] cppr = clusterPermitsToDetectPerRound(clusterPermits, clusterSize);

            assertThat(cppr[0], is(equalTo(clusterPermits)));

            long nextToLastRound = cppr[cppr.length - 2];
            long lastRound = cppr[cppr.length - 1];
            assertThat(nextToLastRound, is(lessThanOrEqualTo(clusterSize)));
            assertThat(nextToLastRound, is(lessThanOrEqualTo(clusterSize)));
            assertThat(lastRound, is(lessThanOrEqualTo(nextToLastRound)));
            assertThat(lastRound, is(lessThanOrEqualTo(clusterSize)));

            assertThat(
                    format("[seed:%d W:%d N:%d, when permits > cluster size, head element == sum of tail elements. ", seed, clusterPermits, clusterSize),
                    stream(cppr).skip(1).sum(),
                    is(equalTo(cppr[0]))
            );

            long[][] allNodesRemainingPerRound = new long[toIntExact(clusterSize)][];
            for (int clusterNode = 0; clusterNode < clusterSize; clusterNode++) {
                allNodesRemainingPerRound[clusterNode] = nodePermitsToDetectPerRound(clusterPermits, clusterNode, clusterSize);
            }

            long sumOfHeads = 0L;
            for (int clusterNode = 0; clusterNode < clusterSize; clusterNode++) {
                long head = allNodesRemainingPerRound[clusterNode][0];
                sumOfHeads += head;
            }

            assertThat(
                    format("[seed:%d W:%d N:%d, head element == sum of node head elements. ", seed, clusterPermits, clusterSize),
                    sumOfHeads,
                    is(equalTo(cppr[0]))
            );

            long sumOfTails = 0L;
            for (int clusterNode = 0; clusterNode < clusterSize; clusterNode++) {
                long[] nodeRemaining = allNodesRemainingPerRound[clusterNode];
                long tailIndex = min(1L, nodeRemaining.length - 1); // 0 or 1
                long sumOfTail = stream(nodeRemaining).skip(tailIndex).sum();

                assertThat(
                        format("[seed:%d W:%d N:%d, node head element == sum of node tail elements. ", seed, clusterPermits, clusterSize),
                        sumOfTail,
                        is(equalTo(nodeRemaining[0]))
                );

                sumOfTails += sumOfTail;
            }

            assertThat(
                    format("[seed:%d W:%d N:%d, head element == sum of all node tail elements. ", seed, clusterPermits, clusterSize),
                    sumOfTails,
                    is(equalTo(cppr[0]))
            );
        }
    }
}