package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.Communicator;
import com.duprasville.limiters.comms.Message;
import com.duprasville.limiters.comms.TestCommunicator;
import com.duprasville.limiters.util.karytree.KaryTree;
import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class TreeFillRateLimiterTest {

    @Test
    void tryAcquireInt() {
        KaryTree karyTree = KaryTree.byMinCapacity(5L, 5L);
        Communicator communicator = new TestCommunicator();
        long nodeId = 3L;
        long clusterSize = karyTree.getCapacity();
        long permitsPerSecond = 5L;

        TreeFillRateLimiter treefill = new TreeFillRateLimiter(permitsPerSecond, nodeId, clusterSize, karyTree, communicator);

        Deque<Message> receivedAtNodeZero = new ConcurrentLinkedDeque<>();
        communicator.getCommNodeById(0L).onReceive((s, d, m) -> receivedAtNodeZero.offer(m));

        boolean acquired = treefill.tryAcquire(2L);
        assertThat(acquired, is(true));
        assertThat(receivedAtNodeZero.size(), equalTo(1));
    }
}