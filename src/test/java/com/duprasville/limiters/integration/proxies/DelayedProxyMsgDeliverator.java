package com.duprasville.limiters.integration.proxies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;

public class DelayedProxyMsgDeliverator extends ProxyMessageDeliverator {

  private List<CachedMsgFuture> cache = new ArrayList<>();

  @Override
  public CompletableFuture<Void> send(Message message) {
    CachedMsgFuture cachedItem = new CachedMsgFuture(message);
    cache.add(cachedItem);
    return cachedItem.future;
  }

  public void releaseMessages() {
    List<CachedMsgFuture> copy = new ArrayList<>(cache);
    cache = new ArrayList<>();
    for(CachedMsgFuture cachedItem : copy) {
      super.send(cachedItem.message)
          .thenApply(result -> {
            //When this future completes, complete the client future...
            cachedItem.future.complete(null);
            return null;
          })
          .exceptionally(e ->
          {
            //When this future completes exceptionally, complete the client exceptionally.
            cachedItem.future.completeExceptionally(new RuntimeException("error", e));
            return cachedItem.future;
          }
      );
    }
  }


  private static class CachedMsgFuture {
    public final Message message;
    public final CompletableFuture<Void> future = new CompletableFuture<>();

    public CachedMsgFuture(Message msg) {
      this.message = msg;
    }
  }
}
