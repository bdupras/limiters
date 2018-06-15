package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageSender;

class MockMessageSender implements MessageSender {
  public List<Message> messagesSent = new ArrayList<>();
  public int messageSentBetweenRateLimiters = 0;
  Map<Long, ClusterRateLimiter> nodesById = new HashMap<>();

  public void addNode(TreeFillRateLimiter treeFillRateLimiter) {
    nodesById.put(treeFillRateLimiter.getId(), treeFillRateLimiter);
  }

  @Override
  public void send(Message message) {
    messagesSent.add(message);
    ClusterRateLimiter src = nodesById.get(message.getSrc());
    ClusterRateLimiter dst = nodesById.get(message.getDst());
    if (src != dst) {
      messageSentBetweenRateLimiters++;
    }
    if (dst != null) {
      dst.receive(message);
    }
  }
}
