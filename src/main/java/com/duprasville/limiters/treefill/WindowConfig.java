package com.duprasville.limiters.treefill;

public class WindowConfig {
    final NodeConfig nodeConfig;
    final long permitsPerSecond;

    public WindowConfig(NodeConfig nodeConfig, long permitsPerSecond) {
        this.nodeConfig = nodeConfig;
        this.permitsPerSecond = permitsPerSecond;
    }
}
