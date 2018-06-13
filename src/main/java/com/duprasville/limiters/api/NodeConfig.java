package com.duprasville.limiters.api;

public class NodeConfig {

  private int totalNodes;
  private int myNodeId;

  //Please add more(STATE ONLY...do not add business objects in to a 'config')

  public NodeConfig(int totalNodes, int myNodeId) {
    this.totalNodes = totalNodes;
    this.myNodeId = myNodeId;
  }

}
