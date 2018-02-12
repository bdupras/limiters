package com.duprasville.limiters.treefill;

import com.duprasville.limiters.util.karytree.KaryTree;

/**
 * Describes this node and its neighborhood within a virtual k-ary tree structure.
 */
class NodeConfig {
    final KaryTree karyTree;
    final long nodeId;
    final long clusterSize;

    final long levelId;
    final long parentNodeId;
    final long parentLevelId;
    final long[] parentLevelNodeIds;
    final long[] children;
    final long leafLevelId;
    final long[] leafNodes;

    final boolean isRootNode;
    final boolean isInnerNode;
    final boolean isLeafNode;

    NodeConfig(KaryTree karyTree, long nodeId, long clusterSize) {
        this.karyTree = karyTree;
        this.nodeId = nodeId;
        this.clusterSize = clusterSize;

        this.levelId = karyTree.levelOfNode(nodeId);
        this.parentNodeId = karyTree.parentOfNode(nodeId);
        this.parentLevelId = karyTree.levelOfNode(parentNodeId);
        this.parentLevelNodeIds = karyTree.nodesOfLevel(parentLevelId);
        this.leafLevelId = karyTree.getLeafLevel();
        this.leafNodes = karyTree.nodesOfLevel(this.leafLevelId);

        this.isRootNode = nodeId == parentNodeId;
        this.isLeafNode = levelId == leafLevelId;
        this.isInnerNode = !this.isLeafNode && !this.isRootNode;

        // TODO: have the tree return empty[] instead of IDs beyond the tree's capacity
        this.children = this.isLeafNode ? new long[]{} : karyTree.childrenOfNode(nodeId);
    }
}
