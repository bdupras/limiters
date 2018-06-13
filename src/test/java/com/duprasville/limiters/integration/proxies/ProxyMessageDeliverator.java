package com.duprasville.limiters.integration.proxies;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;

public class ProxyMessageDeliverator implements MessageDeliverator {

  private Map<Long, DistributedRateLimiter> idToNode = new HashMap<>();


  @Override
  public CompletableFuture<Void> send(Message message) {
    DistributedRateLimiter distributedRateLimiter = idToNode.get(message.getDst());

    //TODO: Insert some stuff in here
    return distributedRateLimiter.receive(message);
  }

  public void setNode(long id, DistributedRateLimiter treeNode1) {
    idToNode.put(id, treeNode1);
  }

}
