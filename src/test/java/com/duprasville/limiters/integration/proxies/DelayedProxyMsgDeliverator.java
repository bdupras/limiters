package com.duprasville.limiters.integration.proxies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;

public class DelayedProxyMsgDeliverator extends ProxyMessageDeliverator {

  private List<CachedMsgFuture> cache = new ArrayList<>();

  volatile boolean allowMessages = false;

  @Override
  public CompletableFuture<Void> send(Message message) {
    CachedMsgFuture cachedItem = new CachedMsgFuture(message);
    if (allowMessages) {
      sendMessage(cachedItem);
    } else {
      cache.add(cachedItem);
    }
    return cachedItem.future;
  }

  public void releaseMessages() {
    allowMessages = true;
    List<CachedMsgFuture> copy = new ArrayList<>(cache);
    cache = new ArrayList<>();
    for (CachedMsgFuture cachedItem : copy) {
      sendMessage(cachedItem);
    }
    allowMessages = false;
  }

  private void sendMessage(CachedMsgFuture msgFuture) {
    try {
      super.send(msgFuture.message)
          .thenApply(result -> {
            //When this future completes, complete the client future...
            msgFuture.future.complete(null);
            return null;
          })
          .exceptionally(e ->
              {
                //When this future completes exceptionally, complete the client exceptionally.
                msgFuture.future.completeExceptionally(new RuntimeException("error", e));
                return msgFuture.future;
              }
          ).get();
    } catch (Exception e) {
      System.out.println("Exception during sending of messages.  Ignoring and continuing.");
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
