package com.duprasville.limiters.treefill;

import com.duprasville.limiters.ClusterRateLimiter;
import com.duprasville.limiters.comms.MessageSource;
import com.duprasville.limiters.util.karytree.KaryTree;

import static java.lang.String.format;

public class TreeFillClusterRateLimiter implements ClusterRateLimiter, TreeFillMessageSink {
    private NodeConfig nodeConfig;
    private volatile long permitsPerSecond;
    private volatile long rounds;

    private final MessageSource messageSource;

    // TODO: create RootNode, InnerNode, and BaseNode specializations?
    public TreeFillClusterRateLimiter(
            long permitsPerSecond,
            long clusterNodeId,
            long clusterSize,
            KaryTree karyTree,
            MessageSource messageSource
    ) {
        reconfigureClusterNode(clusterNodeId, clusterSize, karyTree);
        setRate(permitsPerSecond);
        this.messageSource = messageSource;
    }

    private void reconfigureClusterNode(long clusterNodeId, long clusterSize, KaryTree karyTree) {
        this.nodeConfig = new NodeConfig(karyTree, clusterNodeId, clusterSize);
    }

    @Override
    public boolean tryAcquire(long permits) {
        if (!nodeConfig.isRootNode) {
            messageSource.send(new Inform(nodeConfig.clusterNodeId, nodeConfig.parentNodeId, format("tryAcquire(%d) invoked", permits)));
            messageSource.send(new Detect(nodeConfig.clusterNodeId, nodeConfig.parentNodeId, 0L, permits));
        }
        return true;
    }

    @Override
    public void setRate(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
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


/**
 * Describes this node and its neighborhood within a virtual k-ary tree structure.
 */
class NodeConfig {
    final KaryTree karyTree;
    final long clusterNodeId;
    final long clusterSize;

    final long levelId;
    final long parentNodeId;
    final long parentLevelId;
    final long[] parentLevelNodeIds;
    final long[] childNodeIds;
    final long baseLevelId;
    final long[] baseNodeIds;

    final boolean isRootNode;
    final boolean isBaseNode;

    NodeConfig(KaryTree karyTree, long clusterNodeId, long clusterSize) {
        this.karyTree = karyTree;
        this.clusterNodeId = clusterNodeId;
        this.clusterSize = clusterSize;

        this.levelId = karyTree.levelOfNode(clusterNodeId);
        this.parentNodeId = karyTree.parentOfNode(clusterNodeId);
        this.parentLevelId = karyTree.levelOfNode(parentNodeId);
        this.parentLevelNodeIds = karyTree.nodesOfLevel(parentLevelId);
        this.baseLevelId = karyTree.getBaseLevel();
        this.baseNodeIds = karyTree.nodesOfLevel(this.baseLevelId);

        this.isRootNode = clusterNodeId == parentNodeId;
        this.isBaseNode = levelId != baseLevelId;

        // TODO: have the tree return empty[] instead of IDs beyond the tree's capacity
        this.childNodeIds = this.isBaseNode ? new long[]{} : karyTree.childrenOfNode(clusterNodeId);
    }
}

class WindowConfig {
    final NodeConfig nodeConfig;
    final long clusterPermits;
    final long rounds;
//    final long[] clusterDetectPermitsPerRound;

    WindowConfig(NodeConfig nodeConfig, long clusterPermits) {
        this.nodeConfig = nodeConfig;
        this.clusterPermits = clusterPermits;

        this.rounds = TreeFillMath.rounds(clusterPermits, nodeConfig.clusterSize);
//        this.clusterDetectPermitsPerRound = TreeFillMath.clusterDetectPermitsPerRound(clusterPermits, nodeConfig.clusterSize);
    }
}