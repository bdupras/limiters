package com.duprasville.limiters.vizualization;

import com.duprasville.limiters.util.karytree.KaryTree;

public class KaryNodeAppearance {
    private final KaryTree karyTree;

    public KaryNodeAppearance(KaryTree karyTree) {
        this.karyTree = karyTree;
    }

    public double relX(long nodeId) {
        long levelId = karyTree.levelOfNode(nodeId);
        long width = karyTree.widthOfLevel(levelId);
        long nodeIndex = karyTree.levelIndexOfNode(nodeId); // 0-based index from left side of level
        return (1.0 + nodeIndex) / (1.0 + width); // 1.0's added for lateral margin
    }

    public double relY(long nodeId) {
        long levelId = karyTree.levelOfNode(nodeId);
        long levelMax = karyTree.getHeight() - 1L;
        return (double) levelId / (double) levelMax;
    }

    public double relSize(long id) {
        return Math.max(0.1, 1.0 - (1.0 * relY(id)));
    }
}
