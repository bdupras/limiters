package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.treefill.domain.Detect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class GenericNodeTest {
  private MockMessageDeliverator mockMessageDeliverator;

  @BeforeEach
  void init() {
    mockMessageDeliverator = new MockMessageDeliverator();
  }

  @Test
  void testAcquire() throws ExecutionException, InterruptedException {
    GenericNode node = new GenericNode(1, 1, 10, false, mockMessageDeliverator);
    assertTrue(node.acquire().get());
  }

  @Test
  void testAcquireDetect() {
    GenericNode node = new GenericNode(1, 2, 4, false, mockMessageDeliverator);
    node.acquire();

    assertTrue(!mockMessageDeliverator.messagesSent.isEmpty());
    assertEquals(node.round, 1); // round has advanced
  }

  @Test
  void testReceive() throws ExecutionException, InterruptedException {
    GenericNode node = new GenericNode(1, 1, 10, false, mockMessageDeliverator);
    node.receive(new Detect(1, 1, 0, 1));
    assertTrue(node.selfPermitAllocated);
  }

  @Test
  void testWithOneNodeAndTwoPermitsAllowed() {
    GenericNode node = new GenericNode(1, 1, 2, false, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    node.acquire();
    // because more permits were requested than there is space per round (with one active node),
    // we need to advance the round after we have recorded W / (2^(round - 1) * N) permits
    assertTrue(node.round == 2);

    node.acquire();
    // at this point we have exhausted the amount of requested rate limit quota
    assertTrue(!node.windowOpen);
  }

  private List<GenericNode> buildGraph(int N, int W) {
    List<GenericNode> nodes = new ArrayList<>();
    GenericNode root = new GenericNode(1, N, W, true, mockMessageDeliverator);
    mockMessageDeliverator.addNode(root);
    nodes.add(root);
    GenericNode leftChild = new GenericNode(2, N, W, false, mockMessageDeliverator);
    mockMessageDeliverator.addNode(leftChild);
    nodes.add(leftChild);
    GenericNode rightChild = new GenericNode(3, N, W, false, mockMessageDeliverator);
    mockMessageDeliverator.addNode(rightChild);
    nodes.add(rightChild);

    return nodes;
  }

  private void runMultiTest(boolean acquireOnRoot) {
    GenericNode acquiringNode;

    List<GenericNode> nodes = buildGraph(3, 3);

    GenericNode root = nodes.get(0);
    GenericNode leftChild = nodes.get(1);
    GenericNode rightChild = nodes.get(2);

    if (acquireOnRoot) {
      acquiringNode = root;
    } else {
      acquiringNode = leftChild;
    }

    acquiringNode.acquire();
    assertTrue(root.windowOpen);
    assertTrue(leftChild.windowOpen);
    assertTrue(rightChild.windowOpen);

    acquiringNode.acquire();
    assertTrue(root.windowOpen);
    assertTrue(leftChild.windowOpen);
    assertTrue(rightChild.windowOpen);

    acquiringNode.acquire();
    assertTrue(!root.windowOpen);
    assertTrue(!leftChild.windowOpen);
    assertTrue(!rightChild.windowOpen);

  }

  @Test
  void testWithThreeNodesAndThreePermitsAllowed() {
    runMultiTest(false);
  }

  @Test
  void testSendingToRootWithThreeNodesAndThreePermitsAllowed() {
    runMultiTest(true);
  }

  @Test
  void testSendingToLeftWith12PermitsAndThreeNodes() {
    List<GenericNode> nodes = buildGraph(3, 12);

    GenericNode acquiringNode = nodes.get(0);

    run12PermitGraph(acquiringNode);
  }

  @Test
  void testSendingToLeftWith12PermitsAndThreeNodesWithLeftNode() {
    List<GenericNode> nodes = buildGraph(3, 12);

    GenericNode acquiringNode = nodes.get(1);

    run12PermitGraph(acquiringNode);
  }

  private void run12PermitGraph(GenericNode acquiringNode) {
    acquiringNode.acquire();
    acquiringNode.acquire();
    acquiringNode.acquire();
    acquiringNode.acquire();
    acquiringNode.acquire();

    assertTrue(acquiringNode.windowOpen);

    assertEquals(acquiringNode.round, 1);

    acquiringNode.acquire();

    assertEquals(acquiringNode.round, 2);

    acquiringNode.acquire();
    acquiringNode.acquire();

    assertTrue(acquiringNode.windowOpen);

    acquiringNode.acquire();

    assertEquals(acquiringNode.round, 3);

    acquiringNode.acquire();
    acquiringNode.acquire();

    assertTrue(acquiringNode.windowOpen);

    acquiringNode.acquire();

    assertTrue(!acquiringNode.windowOpen);
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
