package com.duprasville.limiters.treefill;

import com.duprasville.limiters.RateLimiter;
import com.duprasville.limiters.comms.MessageSender;
import com.duprasville.limiters.util.karytree.KaryTree;

import static java.lang.String.format;

public class TreeFillRateLimiter implements RateLimiter {
    private long permitsPerSecond;
    private final long clusterSize;
    private final KaryTree karyTree;
    private final MessageSender messageSender;

    // interesting info about this node within the tree
    private final long nodeId;
    private final long levelId;
    private final long parentId;
    private final long[] childIds;
    private final long baseLevel;
    private final long[] baseIds;

    // locations of a node within a detectTree: root, inner, or base
    private final boolean isRoot;
    private final boolean isBase;

    // TODO: create RootNode, InnerNode, and BaseNode specializations?
    public TreeFillRateLimiter(
            long permitsPerSecond,
            long nodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSender messageSender
    ) {
        this.permitsPerSecond = permitsPerSecond;
        this.clusterSize = clusterSize;
        this.karyTree = karyTree;
        this.messageSender = messageSender;
        this.messageSender.onReceive(receiveMessage);

        this.nodeId = nodeId;
        this.levelId = karyTree.levelOfNode(nodeId);
        this.parentId = karyTree.parentOfNode(nodeId);
        this.baseLevel = karyTree.getBaseLevel();
        this.baseIds = karyTree.nodesOfLevel(this.baseLevel);

        this.isRoot = nodeId == parentId;
        this.isBase = levelId != baseLevel;

        // TODO: have the tree return empty[] instead of IDs beyond the tree's capacity
        this.childIds = this.isBase ? new long[]{} : karyTree.childrenOfNode(nodeId);
    }

    @Override
    public boolean tryAcquire(long permits) {
        if (!isRoot) {
            sendInform(parentId, format("tryAcquire(%d) invoked", permits));
        }
        return true;
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    private final MessageSender.MessageReceiver receiveMessage = (src, dst, msg) -> {
        if (!(msg instanceof Message))
            throw new IllegalStateException("Treefill cannot handle messages of type: " + msg.getClass().getName());
        ((Message) msg).deliver(src, dst, this);
    };

    void sendInform(long dst, String msg) {
        messageSender.send(nodeId, dst, new Inform(msg));
    }

    void receiveInform(long src, long dst, Inform inform) {
        System.out.println(format("Treefill Inform message received. %d -> %d : %s", src, dst, inform.msg));
    }
}
