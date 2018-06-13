package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.api.Message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class GenericNodeTest {
  private MockMessageDeliverator mockMessageDeliverator;

  @BeforeEach
  void init() {
    mockMessageDeliverator = new MockMessageDeliverator();
  }

  @Test
  void testAcquire() throws ExecutionException, InterruptedException {
    GenericNode node = new GenericNode(1, 1, 10, false, mockMessageDeliverator);
    assert (node.acquire().get());
  }

  @Test
  void testAcquireDetect() {
    GenericNode node = new GenericNode(1, 2, 4, false, mockMessageDeliverator);
    node.acquire();

    assert (!mockMessageDeliverator.messagesSent.isEmpty());
    assert (node.round == 1); // round has advanced
  }

  @Test
  void testReceive() throws ExecutionException, InterruptedException {
    GenericNode node = new GenericNode(1, 1, 10, false, mockMessageDeliverator);
    node.receive(new Detect(1, 1, 0, 1));
    assert (node.selfPermitAllocated);
  }

  @Test
  void testWithOneNodeAndTwoPermitsAllowed() {
    GenericNode node = new GenericNode(1, 1, 2, false, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    node.acquire();
    // because more permits were requested than there is space per round (with one active node),
    // we need to advance the round after we have recorded W / (2^(round - 1) * N) permits
    assert (node.round == 2);

    node.acquire();
    // at this point we have exhausted the amount of requested rate limit quota
    assert (!node.windowOpen);
  }

  @Test
  void testWithThreeNodesAndThreePermitsAllowed() {
    GenericNode root = new GenericNode(1, 3, 3, true, mockMessageDeliverator);
    mockMessageDeliverator.addNode(root);
    GenericNode leftChild = new GenericNode(2, 3, 3, false, mockMessageDeliverator);
    mockMessageDeliverator.addNode(leftChild);
    GenericNode rightChild = new GenericNode(3, 3, 3, false, mockMessageDeliverator);
    mockMessageDeliverator.addNode(rightChild);

    leftChild.acquire();

    assert (root.windowOpen);
    assert (leftChild.windowOpen);
    assert (rightChild.windowOpen);

    leftChild.acquire();

    assert (root.windowOpen);
    assert (leftChild.windowOpen);
    assert (rightChild.windowOpen);

    leftChild.acquire();

    assert (!root.windowOpen);
    assert (!leftChild.windowOpen);
    assert (!rightChild.windowOpen);
  }

  class MockMessageDeliverator implements MessageDeliverator {
    public List<Message> messagesSent = new ArrayList<>();
    Map<Long, DistributedRateLimiter> nodesById = new HashMap<>();


    public void addNode(GenericNode genericNode) {
      nodesById.put(genericNode.getId(), genericNode);
    }

    @Override
    public CompletableFuture<Void> send(Message message) {
      messagesSent.add(message);
      DistributedRateLimiter dest = nodesById.get(message.getDst());

      if (dest != null) dest.receive(message).thenApply(s -> null);

      return CompletableFuture.completedFuture(null);
    }
  }
}
