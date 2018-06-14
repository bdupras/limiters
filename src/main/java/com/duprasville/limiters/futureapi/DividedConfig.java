package com.duprasville.limiters.futureapi;

public class DividedConfig {
    public final long clusterSize;
    public final double permitsPerSecond;

    public DividedConfig(long clusterSize, double permitsPerSecond) {
        this.clusterSize = clusterSize;
        this.permitsPerSecond = permitsPerSecond;
    }
}
