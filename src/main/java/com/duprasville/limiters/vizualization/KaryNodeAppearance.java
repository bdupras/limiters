package com.duprasville.limiters.vizualization;

import com.duprasville.limiters.util.karytree.KaryTree;

import java.util.Random;
public class KaryNodeAppearance {
    private final KaryTree karyTree;

    public KaryNodeAppearance(KaryTree karyTree) {
        this.karyTree = karyTree;
    }

    public double[] relXYtree(long nodeId) {
        long levelId = karyTree.levelOfNode(nodeId);
        long width = karyTree.widthOfLevel(levelId);
        long nodeIndex = karyTree.levelIndexOfNode(nodeId); // 0-based index from left side of level
        double x = (2.0 + nodeIndex) / (3.0 + width); // 1.0's added for lateral margin
        double levelMax = karyTree.getHeight() - 1L;
        double y = levelId / levelMax;

        double a = 0.0;
        double r = 0.0;
        return new double[]{x, y, a, r};
    }

    private static Random random = new Random();
    public double[] relXrandY(long nodeId) {
        long parent = karyTree.parentOfNode(nodeId);
        double[] parentTreeCoord = relXYtree(parent);
        double[] nodeTreeCoord = relXYtree(nodeId);
        double bottomY = nodeTreeCoord[1];
        double distanceYToParent = bottomY - parentTreeCoord[1];
        double rangeY = 0.5 * distanceYToParent;
        double randY = bottomY - (random.nextDouble() * rangeY);
        nodeTreeCoord[1] = randY;
        return nodeTreeCoord;
    }

    public double[] relXY(long nodeId) {
        long nodeLevel = karyTree.levelOfNode(nodeId);
        long maxLevel = karyTree.getHeight() - 1L;
        if (nodeLevel < maxLevel) {
            return relXYtree(nodeId);
        } else {
            return relXrandY(nodeId);
        }
    }

    public double relSize(long nodeId) {
        return Math.max(0.2, 1.0 - (1.0 * relXYtree(nodeId)[1]));
    }
}
