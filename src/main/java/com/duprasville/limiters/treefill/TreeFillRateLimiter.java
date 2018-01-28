package com.duprasville.limiters.treefill;

import com.duprasville.limiters.RateLimiter;
import com.duprasville.limiters.comms.CommNode;
import com.duprasville.limiters.comms.Communicator;
import com.duprasville.limiters.comms.Message;
import com.duprasville.limiters.util.karytree.KaryTree;

import static java.lang.String.format;

public class TreeFillRateLimiter implements RateLimiter {
    private long permitsPerSecond;
    private final long nodeId;
    private final long clusterSize;
    private final KaryTree karyTree;
    private final Communicator communicator;
    private final long parentNodeId;
    private final boolean isRoot;
    private final CommNode thisCommNode;
    private final CommNode parentCommNode;

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

        this.parentNodeId = karyTree.parentOfNode(nodeId);
        this.isRoot = nodeId == parentNodeId;
        this.thisCommNode = communicator.getCommNodeById(this.nodeId);
        this.parentCommNode = communicator.getCommNodeById(this.parentNodeId);
    }

    @Override
    public boolean tryAcquire(long permits) {
        if (!isRoot) {
            Message msg = communicator.newMessage(format("%s tryAcquire(%d) invoked", this, permits));
            communicator.sendTo(thisCommNode, parentCommNode, msg);
        }
        return true;
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }
}
