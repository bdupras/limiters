package com.duprasville.limiters.treefill;

import com.duprasville.limiters.RateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.karytree.KaryTree;

import static java.lang.String.format;

public class TreeFillRateLimiter implements RateLimiter, TreeFillMessageSink {
    private volatile long permitsPerSecond;
    private volatile long rounds;

    private final long clusterSize;
    private final KaryTree karyTree;
    private final MessageSource messageSource;

    // interesting info about this node within the tree
    private final long nodeId;
    private final long levelId;
    private final long parentId;
    private final long parentLevelId;
    private final long[] parentLevelNodeIds;
    private final long[] childIds;
    private final long baseLevelId;
    private final long[] baseNodeIds;

    // locations of a node within a detectTree: root, inner, or base
    private final boolean isRoot;
    private final boolean isBase;

    // TODO: create RootNode, InnerNode, and BaseNode specializations?
    public TreeFillRateLimiter(
            long permitsPerSecond,
            long nodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSource messageSource
    ) {
        this.permitsPerSecond = permitsPerSecond;
        this.clusterSize = clusterSize;
        this.karyTree = karyTree;
        this.messageSource = messageSource;

        this.nodeId = nodeId;
        this.levelId = karyTree.levelOfNode(nodeId);
        this.parentId = karyTree.parentOfNode(nodeId);
        this.parentLevelId = karyTree.levelOfNode(parentId);
        this.parentLevelNodeIds = karyTree.nodesOfLevel(parentLevelId);
        this.baseLevelId = karyTree.getBaseLevel();
        this.baseNodeIds = karyTree.nodesOfLevel(this.baseLevelId);

        this.isRoot = nodeId == parentId;
        this.isBase = levelId != baseLevelId;

        // TODO: have the tree return empty[] instead of IDs beyond the tree's capacity
        this.childIds = this.isBase ? new long[]{} : karyTree.childrenOfNode(nodeId);
    }

    @Override
    public boolean tryAcquire(long permits) {
        if (!isRoot) {
            messageSource.send(new Inform(nodeId, parentId, format("tryAcquire(%d) invoked", permits)));
            messageSource.send(new Detect(nodeId, parentId, 0L, permits));
        }
        return true;
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
        double logOfWoverN = karyTree.log((double)permitsPerSecond / (double)clusterSize);
        this.rounds = (long)(Math.ceil(logOfWoverN));
    }

    @Override
    public void receive(Inform inform) {
        System.out.println(inform);
    }

    @Override
    public void receive(Detect detect) {
        System.out.println(detect);
    }
}
