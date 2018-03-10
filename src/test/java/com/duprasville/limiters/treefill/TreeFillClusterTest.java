package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.Message;
import com.duprasville.limiters.comms.TestMessageSource;
import com.duprasville.limiters.testutil.TestTicker;
import com.duprasville.limiters.util.karytree.KaryTree;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.Math.abs;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TreeFillClusterTest {
    @Test
    void treefill_5x4_1248_1() {
        KaryTree karyTree = KaryTree.byHeight(5, 3);
        long clusterSize = karyTree.getCapacity();
        long requestsToSend = clusterSize * 2;
        long permitsPerRequest = 1L;
        long permitsPerSecond = requestsToSend * permitsPerRequest;
        Map<Long, TreeFillClusterRateLimiter> cluster = new ConcurrentHashMap<>();
        TestTicker ticker = new TestTicker();
        long seed = 0xDEADBEEF; //new Random().nextLong();
        Random testRandom = new Random(seed);
        LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

        for (long nodeId = 0; nodeId < clusterSize; nodeId++) {
            Random nodeRandom = new Random(testRandom.nextLong());
            TestMessageSource messageSource = new TestMessageSource(new Random(testRandom.nextLong()));
            TreeFillClusterRateLimiter node = new TreeFillClusterRateLimiter(
                    permitsPerSecond,
                    nodeId,
                    clusterSize,
                    karyTree,
                    messageSource,
                    ticker,
                    nodeRandom
            );
            messageSource.onSend(messageQueue::add);
            cluster.put(nodeId, node);
        }

        cluster.forEach((nodeId, node) -> {
            WindowConfig cfg = node.windowConfig;

            assertThat(cfg.rounds.length, is(equalTo(2)));
            assertThat(cfg.nodePermitsPerRound.length, is(equalTo(3)));
        });


        // before 1st acquire
        long[] cppr = {62L, 31L, 31L};
        long[] nppr = {2L, 1L, 1L};
        int dummyDetectsRound1 = cluster.values().stream().map(n ->
                n.currentWindow().detectsTable.getRow(1L).stream()
                        .filter(Objects::nonNull)
                        .collect(toList()).size()).mapToInt(v -> v).sum();

        int dummyFullsRound1 = cluster.values().stream().map(n ->
                n.currentWindow().fullsReceived.getRow(1L).stream()
                        .filter(Objects::nonNull)
                        .collect(toList()).size()).mapToInt(v -> v).sum();

        int dummyDetectsRound2 = cluster.values().stream().map(n ->
                n.currentWindow().detectsTable.getRow(2L).stream()
                        .filter(Objects::nonNull)
                        .collect(toList()).size()).mapToInt(v -> v).sum();

        int dummyFullsRound2 = cluster.values().stream().map(n ->
                n.currentWindow().fullsReceived.getRow(2L).stream()
                        .filter(Objects::nonNull)
                        .collect(toList()).size()).mapToInt(v -> v).sum();
        assertThat(dummyDetectsRound1, is(equalTo(19)));
        assertThat(dummyFullsRound1, is(equalTo(0)));
        assertThat(dummyDetectsRound2, is(equalTo(19)));
        assertThat(dummyFullsRound2, is(equalTo(0)));

        cluster.values().forEach((node) ->
                assertArrayEquals(nppr, node.windowConfig.nodePermitsPerRound));

        long requestsSent = 0L;
        long permitsRequested = 0;
        long requestsAccepted = 0L;
        long requestsRejected = 0L;
        long permitsAcquired = 0L;

        boolean acquired = true;
        long nextNode = clusterSize - 1;


        while (acquired) {
            TreeFillClusterRateLimiter node = cluster.get(nextNode);

            acquired = node.tryAcquire(permitsPerRequest);

            while (!messageQueue.isEmpty()) {
                List<Message> messages = new ArrayList<>();
                messageQueue.drainTo(messages);
                messages.forEach(m ->
                        cluster.get(m.getDst()).receive(m));
            }

            requestsSent++;
            long permitsAquired;
            switch (toIntExact(requestsSent)) {
                case 31:
                    int countDetects = cluster.values().stream().flatMap(n -> n.currentWindow().detectsTable.getRow(1L).stream().filter(Objects::nonNull)).collect(toList()).size();
                    assertThat(countDetects, is(equalTo(31)));
                    permitsAquired = cluster.values().stream().flatMap(n -> n.currentWindow().detectsTable.getRow(1L).stream().filter(Objects::nonNull)).mapToLong(v -> v.permitsAcquired).sum();
                    assertThat(permitsAquired, is(equalTo(31L)));
                    break;
                case 62:
                    long countDetects2 = cluster.values().stream().map(n -> n.currentWindow().detectsTable.getRow(2L).stream().filter(Objects::nonNull).collect(toList()).size()).mapToLong(l -> l).sum();
                    assertThat(countDetects2, is(equalTo(50L)));
                    permitsAquired = cluster.values().stream().flatMap(n -> n.currentWindow().detectsTable.getRow(2L).stream().filter(Objects::nonNull)).mapToLong(v -> v.permitsAcquired).sum();
                    assertThat(permitsAquired, is(equalTo(62L)));
                    break;
            }
            permitsRequested += permitsPerRequest;
            if (acquired) {
                requestsAccepted++;
                permitsAcquired += permitsPerRequest;
            } else {
                requestsRejected++;
            }
            nextNode = abs(--nextNode % clusterSize);
        }
        System.out.println(format(
                "requestsSent %d, permitsRequeste %d, requestsAccepted %d, requestsRejected %d, permitsAcquired %d",
                requestsSent, permitsRequested, requestsAccepted, requestsRejected, permitsAcquired
        ));

        //assertThat(acquired, is(true));
//        assertThat(permitsAcquired, is(equalTo(requestsToSend * permitsPerRequest)));
    }
}
