package com.duprasville.limiters.futureapi;

public class TreeFillConfig {
  public final long nodeId;
  public final long clusterSize;
  public final long permitsPerSecond;

  public TreeFillConfig(
          long nodeId,
          long clusterSize,
          long permitsPerSecond
  ) {
    this.nodeId = nodeId;
    this.clusterSize = clusterSize;
    this.permitsPerSecond = permitsPerSecond;
  }
}
