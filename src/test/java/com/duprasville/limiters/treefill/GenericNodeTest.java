package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.api.Node;
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
  void testWithTwoNodesAndFourPermitsAllowed() {
    GenericNode node = new GenericNode(1, 2, 4, false, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    node.acquire();
    assert (node.round == 2);
  }

//  @Test
//  void testWithThreeNodesAndFourPermitsAllowed() {
//    GenericNode root = new GenericNode(1, 3, 4, true, mockMessageDeliverator);
//    mockMessageDeliverator.addNode(root);
//    GenericNode leftChild = new GenericNode(2, 3, 4, false, mockMessageDeliverator);
//    mockMessageDeliverator.addNode(leftChild);
//    GenericNode rightChild = new GenericNode(3, 3, 4, false, mockMessageDeliverator);
//    mockMessageDeliverator.addNode(rightChild);
//
//    leftChild.acquire();
//
//    assert (root.windowOpen);
//    assert (leftChild.windowOpen);
//    assert (rightChild.windowOpen);
//
//    leftChild.acquire();
//
//    assert (root.windowOpen);
//    assert (leftChild.windowOpen);
//    assert (rightChild.windowOpen);
//
//    leftChild.acquire();
//
//    assert (root.windowOpen);
//    assert (leftChild.windowOpen);
//    assert (rightChild.windowOpen);
//
//    leftChild.acquire();
////  WHAT SHOULD HAPPEN HERE:
  /// IF WE ARE ROOT AND WE KNOW OUR CHILDREN ARE FULL WE NEED TO CLOSE THE WINDOW REGARDLESS OF
  // WHETHER MAX PERMITS W IS REACHED SINCE THAT IS CLUSTER CAPACITY
  // I ALSO WANT TO REEVALUATE HOW WE PICK THE NUMBER OF DETECTS / PERMITS ALLOWED PER NODE
//    assert (!root.windowOpen);
//    assert (!leftChild.windowOpen);
//    assert (!rightChild.windowOpen);
//  }

  class MockMessageDeliverator implements MessageDeliverator {
    public List<Message> messagesSent = new ArrayList<>();
    Map<Long, Node> nodesById = new HashMap<>();


    public void addNode(Node node) {
      nodesById.put(node.getId(), node);
    }

    @Override
    public CompletableFuture<Void> send(Message message) {
      messagesSent.add(message);
      Node dest = nodesById.get(message.getDst());

      if (dest != null) dest.receive(message).thenApply(s -> null);

      return CompletableFuture.completedFuture(null);
    }
  }
}
