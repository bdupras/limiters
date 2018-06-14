package com.duprasville.limiters.futureapi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;

public interface FutureMessageDeliverator extends FutureMessageSender, FutureMessageReceiver {
  FutureMessageDeliverator NIL = new FutureMessageDeliverator() {
    @Override
    public CompletableFuture<Void> receive(Message message) {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> send(Message message) {
      return CompletableFuture.completedFuture(null);
    }
  };

  @Override
  default CompletableFuture<Void> receive(Message message) {
    return FutureMessageReceiver.NIL.receive(message);
  }

  static MessageDeliverator toMessageDeliverator(FutureMessageDeliverator futureMessageDeliverator) {
      return new MessageDeliverator() {
        final FutureMessageDeliverator delegate = futureMessageDeliverator;

        @Override
        public void receive(Message message) {
          try {
            delegate.receive(message).get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        }

        @Override
        public void send(Message message) {
          try {
            delegate.send(message).get();
          } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
          }
        }
      };
  }
}
