package com.duprasville.limiters.treefill;

import java.util.stream.LongStream;

import static com.duprasville.limiters.treefill.TreeFillMath.nodePermitsToDetectPerRound;
import static java.lang.Math.toIntExact;

class WindowConfig {
    final NodeConfig nodeConfig;
    final long[] rounds;
    final long[] nodePermitsPerRound;
    final long clusterPermits;
    final DetectTable detectTableTemplate;
    public FullTable fullTableTemplate;

    WindowConfig(NodeConfig nodeConfig, long clusterPermits) {
        this.nodeConfig = nodeConfig;
        this.clusterPermits = clusterPermits;
        this.nodePermitsPerRound = nodePermitsToDetectPerRound(clusterPermits, nodeConfig.nodeId, nodeConfig.clusterSize);

        this.rounds = LongStream.range(1, nodePermitsPerRound.length).toArray(); // [1L, 2L, ...]
        this.detectTableTemplate = nodeConfig.isLeafNode ? new DetectTable(rounds) : DetectTable.NIL;
        this.fullTableTemplate = nodeConfig.isLeafNode ? FullTable.NIL : new FullTable(rounds, nodeConfig.children);
    }

    long getPermitsToDetectPerRound(long round) {
        return nodePermitsPerRound[toIntExact(round)];
    }
}
