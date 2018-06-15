package com.duprasville.limiters.integration.proxies;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.duprasville.limiters.api.Message;

public class DelayedProxyMessageSender extends ProxyMessageSender {

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
    return CompletableFuture.completedFuture(null);
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
      super.send(msgFuture.message);
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
