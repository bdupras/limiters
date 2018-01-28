package com.duprasville.limiters.treefill;

import com.duprasville.limiters.RateLimiter;
import com.duprasville.limiters.comms.CommNode;
import com.duprasville.limiters.comms.Communicator;
import com.duprasville.limiters.comms.Message;
import com.duprasville.limiters.comms.MessageSender;
import com.duprasville.limiters.util.karytree.KaryTree;

import static java.lang.String.format;

public class TreeFillRateLimiter implements RateLimiter {
    private long permitsPerSecond;
    private final long nodeId;
    private final long clusterSize;
    private final KaryTree karyTree;
    private final MessageSender messageSender;
    private final long parentNodeId;
    private final boolean isRoot;

    public TreeFillRateLimiter(
            long permitsPerSecond,
            long nodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSender messageSender
    ) {
        this.permitsPerSecond = permitsPerSecond;
        this.nodeId = nodeId;
        this.clusterSize = clusterSize;
        this.karyTree = karyTree;
        this.messageSender = messageSender;

        this.parentNodeId = karyTree.parentOfNode(nodeId);
        this.isRoot = nodeId == parentNodeId;
    }

    @Override
    public boolean tryAcquire(long permits) {
        if (!isRoot) {
            messageSender.send(nodeId, parentNodeId, format("%s tryAcquire(%d) invoked", this, permits));
        }
        return true;
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }
}
