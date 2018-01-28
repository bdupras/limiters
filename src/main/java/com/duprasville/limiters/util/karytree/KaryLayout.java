package com.duprasville.limiters.util.karytree;

public class KaryLayout {
    private final KaryTree karyTree;

    public KaryLayout(KaryTree karyTree) {
        this.karyTree = karyTree;
    }

    public double relX(long id) {
        KaryTree.Node node = karyTree.getNode(id);
        KaryTree.Node leftNode = node.getLevel().getNodes().getMin();
        long nodeIndex = node.getId() - leftNode.getId(); // 0-based index from left side of level
        return (1.0 + nodeIndex) / (1.0 + node.getLevel().getWidth()); // 1's added for lateral margin
    }

    public double relY(long id) {
        int levelIndex = karyTree.getNode(id).getLevel().getId();
        int levelMax = karyTree.getBase().getId();
        return (double)levelIndex / (double)levelMax;
    }
}
