package com.duprasville.limiters.integration.proxies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.futureapi.DistributedRateLimiter;
import com.duprasville.limiters.futureapi.FutureMessageSender;

public class ProxyMessageSender implements FutureMessageSender {

  protected Map<Long, DistributedRateLimiter> idToNode = new HashMap<>();

  @Override
  public CompletableFuture<Void> send(Message message) {
    DistributedRateLimiter distributedRateLimiter = idToNode.get(message.getDst());

    //TODO: Insert some stuff in here
    return distributedRateLimiter.receive(message);
  }

  public void setNode(long id, DistributedRateLimiter treeNode1) {
    idToNode.put(id, treeNode1);
  }

  public boolean acquireSingle(long nodeId) {
    List<CompletableFuture<Boolean>> completableFutures = acquireAsync(nodeId, 1);
    CompletableFuture<Boolean> future = completableFutures.get(0);
    try {
      return future.get(2, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException("Future should be completed.  bug in impl (or test)", e);
    }
  }

  public List<CompletableFuture<Boolean>> acquireAsync(long nodeId, int numPermits) {
    DistributedRateLimiter limiter = idToNode.get(nodeId);

    List<CompletableFuture<Boolean>> results = new ArrayList<>();
    for (int i = 0; i < numPermits; i++) {
      results.add(limiter.acquire());
    }
    return results;
  }

  public void acquireOrFailSynchronous(long nodeId, int numPermits) {
    List<CompletableFuture<Boolean>> futures = acquireAsync(nodeId, numPermits);
    for (CompletableFuture<Boolean> future : futures) {
      //This should complete immediately if a test is calling this method
      try {
        Boolean acquired = future.get(2, TimeUnit.SECONDS);
        if (!acquired)
          throw new IllegalStateException("Test expects lock to be acquired and it was not");
      } catch (Exception e) {
        throw new RuntimeException("test failed, this should not timeout for this test", e);
      }
    }
  }


}
