package com.duprasville.limiters.treefill;

import com.duprasville.limiters.RateLimiter;
import com.duprasville.limiters.comms.Communicator;
import com.duprasville.limiters.comms.Node;
import com.duprasville.limiters.util.karytree.KaryTree;

public class TreeFillRateLimiter implements RateLimiter {
    private long permitsPerSecond;
    private final long nodeId;
    private final long clusterSize;
    private final KaryTree karyTree;
    private final Communicator communicator;
    private final Node parent;

    public TreeFillRateLimiter(
            long permitsPerSecond,
            long nodeId,
            long clusterSize,
            KaryTree karyTree,
            Communicator communicator
    ) {
        this.permitsPerSecond = permitsPerSecond;
        this.nodeId = nodeId;
        this.clusterSize = clusterSize;
        this.karyTree = karyTree;
        this.communicator = communicator;

        this.parent = communicator.getNodeById(-1L);
    }


    @Override
    public boolean tryAcquire(int permits) {
        return false;
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }
}
