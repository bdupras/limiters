package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.Message;
import com.duprasville.limiters.util.karytree.KaryTree;

import java.util.Random;
import java.util.stream.LongStream;

import static com.duprasville.limiters.treefill.TreeFillMath.clusterPermitsToDetectPerRound;
import static com.duprasville.limiters.treefill.TreeFillMath.nodePermitsToDetectPerRound;
import static com.duprasville.limiters.util.Utils.spread;
import static java.lang.Math.toIntExact;

class WindowConfig {
    final NodeConfig nodeConfig;
    final long[] rounds;
    final long[] nodePermitsPerRound;
    final long clusterPermits;
    private final Random random;
    final DetectTable detectTableTemplate;
    final FullTable fullTableTemplate;
    final long[] clusterPermitsPerRound;

    WindowConfig(NodeConfig nodeConfig, long clusterPermits, Random random) {
        this.nodeConfig = nodeConfig;
        this.clusterPermits = clusterPermits;
        this.random = random;
        this.nodePermitsPerRound = nodePermitsToDetectPerRound(
                clusterPermits, nodeConfig.nodeId, nodeConfig.clusterSize);
        this.clusterPermitsPerRound = clusterPermitsToDetectPerRound(
                clusterPermits, nodeConfig.clusterSize);

        this.rounds = LongStream.range(1, nodePermitsPerRound.length).toArray(); // [1L, 2L, ...]
        this.detectTableTemplate = nodeConfig.isLeafNode ? createDetectTable() : DetectTable.NIL;
        this.fullTableTemplate = nodeConfig.isLeafNode ? FullTable.NIL : createFullTable();
    }

    /*
     * In the final rounds, the number of permits to detect is fewer than the number of detector nodes.
     * Some detector nodes' "fair share" of permits to detect will be 0. These creator methods account
     * for this case.
     *
     * Detector nodes get their detect tables pre-populated wth zero-permit Detect messages.
     *
     * Filler nodes must account for descendant nodes' rounds which are completely pre-filled.
     *
     * This most extreme case of this is when a round must detect a single permit, in which case:
     *   - all but one detector node will be completely full
     *   - the remaining detector node will have all but one Detect slot filled
     *   - filler nodes which /are not/ ancestors of the remaining detector will be completely full
     *   - filler nodes which /are/ ancestors of the remaining detector will have all but one Full slot filled
     *
     * This may seem like a lot of computation, especially for filler nodes, but it is performed only at init
     * or when the cluster is reconfigured.
     */

    long getPermitsToDetectPerRound(long round) {
        return getPermitsToDetectPerRound(nodePermitsPerRound, round);
    }

    private DetectTable createDetectTable() {
        return createDetectTableFor(nodeConfig, this);
    }

    private FullTable createFullTable() {
        return createFullTableFor(nodeConfig, this);
    }


    private static long getPermitsToDetectPerRound(long[] permitsPerRound, long round) {
        return permitsPerRound[toIntExact(round)];
    }

    private static DetectTable createDetectTableFor(NodeConfig nodeConfig, WindowConfig windowConfig) {
        DetectTable detectTable = new DetectTable(windowConfig.rounds, nodeConfig.karyTree.getAry());
        for (long round : windowConfig.rounds) { // round = 1, 2, ..., I
            long cppr = windowConfig.clusterPermitsPerRound[toIntExact(round)];
            long dummies = nodeConfig.clusterSize - cppr;
            if (dummies > 0L) {
                long nodeDummies = spread(dummies, nodeConfig.nodeId, nodeConfig.clusterSize);
                for (long d = 0; d < nodeDummies; d++) {
                    detectTable.tryPut(createDummyDetect(nodeConfig.nodeId, round));
                }
            }
        }
        return detectTable;
    }

    private static Detect createDummyDetect(long nodeId, long round) {
        return new Detect(nodeId, nodeId, -1L, round, 0L);
    }

    private static FullTable createFullTableFor(NodeConfig nodeConfig, WindowConfig windowConfig) {
        // TODO when making this work for imperfect trees, "detector nodes" and "filler nodes" may not have a
        // child/parent relationship
        long[] children = nodeConfig.karyTree.childrenOfNode(nodeConfig.nodeId);
        FullTable fullTable = new FullTable(windowConfig.rounds, children, windowConfig.random);
        for (long child : children) {
            MessageTable<? extends Message> childMessageTable;
            NodeConfig childNodeConfig = new NodeConfig(nodeConfig.karyTree, child, nodeConfig.clusterSize);
            WindowConfig childWindowConfig = new WindowConfig(childNodeConfig, windowConfig.clusterPermits, windowConfig.random);
            if (nodeConfig.karyTree.levelOfNode(child) == nodeConfig.karyTree.getLeafLevel()) {
                childMessageTable = createDetectTableFor(childNodeConfig, childWindowConfig);
            } else {
                childMessageTable = createFullTableFor(childNodeConfig, childWindowConfig);
            }

            for (long round : windowConfig.rounds) {
                if (childMessageTable.isRowFull(round)) {
                    fullTable.tryPut(createDummyFull(nodeConfig.nodeId, child, round));
                }
            }
        }
        return fullTable;
    }

    private static Full createDummyFull(long nodeId, long child, long round) {
        return new Full(child, nodeId, -1, round, 0L);
    }

}
