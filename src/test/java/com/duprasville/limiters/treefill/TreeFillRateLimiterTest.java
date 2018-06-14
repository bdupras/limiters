package com.duprasville.limiters.treefill;

import com.duprasville.limiters.api.ClusterRateLimiter;
import com.duprasville.limiters.api.FutureSerialDeliverator;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.testutil.SameThreadExecutorService;
import com.duprasville.limiters.testutil.TestTicker;
import com.duprasville.limiters.treefill.domain.Acquire;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.treefill.domain.TreeFillMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TreeFillRateLimiterTest {
  private MockMessageDeliverator mockMessageDeliverator;
  private TestTicker ticker;
  private ExecutorService executor;

  @BeforeEach
  void init() {
    this.mockMessageDeliverator = new MockMessageDeliverator();
    this.ticker = new TestTicker(0L);
    this.executor = new SameThreadExecutorService();
  }

  @Test
  void testAcquire() throws ExecutionException, InterruptedException {
    TreeFillRateLimiter node = new TreeFillRateLimiter(1, 1, 8, ticker, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);

    assertTrue(node.acquire());
  }

  @Test
  void testAcquireDetect() {
    TreeFillRateLimiter node = new TreeFillRateLimiter(1, 2, 4, ticker, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    node.acquire();

    assertTrue(!mockMessageDeliverator.messagesSent.isEmpty());
    assertEquals(node.currentWindow().round, 1); // round has advanced
  }

  @Test
  void testReceive() throws ExecutionException, InterruptedException {
    TreeFillRateLimiter node = new TreeFillRateLimiter(1, 1, 32, ticker, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);

    node.receive(new Detect(1, 1, 1, 1));
    assertEquals(node.currentWindow().round, 2);
  }

  @Test
  void testWithOneNodeAndTwoPermitsAllowed() {
    TreeFillRateLimiter node = new TreeFillRateLimiter(1, 1, 2, ticker, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    node.acquire();
    // because more permits were requested than there is space per round (with one active node),
    // we need to advance the round after we have recorded W / (2^(round - 1) * N) permits
    assertTrue(node.currentWindow().round == 2);

    node.acquire();
    // at this point we have exhausted the amount of requested rate limit quota
    assertTrue(!node.currentWindow().windowOpen);
  }

  @Test
  void testMultiAcquireWithOneNodeAndFourPermits() {
    TreeFillRateLimiter node = new TreeFillRateLimiter(1, 1, 4, ticker, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    node.acquire(2);

    assertEquals(2, node.currentWindow().round);

    node.acquire(2);
    assertFalse(node.currentWindow().windowOpen);
  }

  @Test
  void test6AcquiresWithOneNodeAndFourPermits() throws ExecutionException, InterruptedException {
    TreeFillRateLimiter node = new TreeFillRateLimiter(1, 1, 4, ticker, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    node.acquire(3);

    assertEquals(3, node.currentWindow().round);

    assertFalse(node.acquire(3));
    assertTrue(node.currentWindow().windowOpen);
  }

  @Test
  void testExcessiveMultiAcquireWithOneNodeAndFourPermits() throws ExecutionException, InterruptedException {
    TreeFillRateLimiter node = new TreeFillRateLimiter(1, 1, 4, ticker, mockMessageDeliverator);
    mockMessageDeliverator.addNode(node);
    assertFalse(node.acquire(5));
  }

  @Test
  void testATreeWith4Levels() throws ExecutionException, InterruptedException {
    List<TreeFillRateLimiter> nodes = buildGraph(10, 40);
    TreeFillRateLimiter node = nodes.get(3);
    assertTrue(node.acquire(4));
    assertTrue(node.acquire(2));

    TreeFillRateLimiter node2 = nodes.get(1);
    assertFalse(node2.currentWindow().selfPermitAllocated);
    assertTrue(node2.currentWindow().childPermitsAllocated[0]);
  }

  private List<TreeFillRateLimiter> buildGraph(int N, int W) {
    List<TreeFillRateLimiter> nodes = new ArrayList<>();

    for (int i = 1; i <= N; i++) {
      TreeFillRateLimiter nodelet = new TreeFillRateLimiter(i, N, W, ticker, mockMessageDeliverator);
      mockMessageDeliverator.addNode(nodelet);
      nodes.add(nodelet);
    }

    return nodes;
  }

  private void runMultiTest(boolean acquireOnRoot) throws Exception {
    TreeFillRateLimiter acquiringNode;

    List<TreeFillRateLimiter> nodes = buildGraph(3, 3);

    TreeFillRateLimiter root = nodes.get(0);
    TreeFillRateLimiter leftChild = nodes.get(1);
    TreeFillRateLimiter rightChild = nodes.get(2);

    if (acquireOnRoot) {
      acquiringNode = root;
    } else {
      acquiringNode = leftChild;
    }

    acquiringNode.acquire();
    assertTrue(root.currentWindow().windowOpen);
    assertTrue(leftChild.currentWindow().windowOpen);
    assertTrue(rightChild.currentWindow().windowOpen);

    acquiringNode.acquire();
    assertTrue(root.currentWindow().windowOpen);
    assertTrue(leftChild.currentWindow().windowOpen);
    assertTrue(rightChild.currentWindow().windowOpen);

    acquiringNode.acquire();
    assertTrue(!root.currentWindow().windowOpen);
    assertTrue(!leftChild.currentWindow().windowOpen);
    assertTrue(!rightChild.currentWindow().windowOpen);

  }

  @Test
  void testWithThreeNodesAndThreePermitsAllowed() throws Exception {
    runMultiTest(false);
  }

  @Test
  void testSendingToRootWithThreeNodesAndThreePermitsAllowed() throws Exception {
    runMultiTest(true);
  }

  @Test
  void testSendingToLeftWith12PermitsAndThreeNodes() {
    List<TreeFillRateLimiter> nodes = buildGraph(3, 12);

    TreeFillRateLimiter acquiringNode = nodes.get(0);

    run12PermitGraph(acquiringNode);
  }

  @Test
  void testSendingToLeftWith12PermitsAndThreeNodesWithLeftNode() {
    List<TreeFillRateLimiter> nodes = buildGraph(3, 12);

    TreeFillRateLimiter acquiringNode = nodes.get(1);

    run12PermitGraph(acquiringNode);
  }

  @Test
  void testSendingOverTheRateLimitW() throws ExecutionException, InterruptedException {
    List<TreeFillRateLimiter> nodes = buildGraph(3, 12);
    TreeFillRateLimiter acquiringNode = nodes.get(1);
    acquiringNode.acquire(6);
    // only 6 remaining
    assertEquals(2, acquiringNode.currentWindow().round);

    boolean result = acquiringNode.acquire(7);
    assertFalse(result);
  }

  @Test
  void testNodesWithExtraAcquiresWhenRoundIncrements() {
    List<TreeFillRateLimiter> nodes = buildGraph(3, 12);

    TreeFillRateLimiter node1 = nodes.get(1);
    TreeFillRateLimiter node2 = nodes.get(2);

    for (int i = 0; i < 4; i++) {
      node1.acquire();
    }

    node2.acquire();

    node1.acquire();
    node1.acquire(); // round full with one left at node2

    assertEquals(node2.currentWindow().round, 2);
    assertTrue(node2.currentWindow().selfPermitAllocated);
  }

  @Test
  void testRaceBetweenPastDetectAndRoundIncrement() {
    TreeFillRateLimiter root = new TreeFillRateLimiter(1, 3, 12, ticker, mockMessageDeliverator);
    WindowState window = root.currentWindow();
    window.round = 2;
    window.receive(
        new Detect(2, 1, 1, 4)
    );

    // what *should* occur here, after we receive the late detect, is splitting up the permits
    // acquired so they fit inside our share this round.

    List<Message> messagesSent = mockMessageDeliverator.messagesSent;
    assertEquals(2, messagesSent.size());
    for (Message message : messagesSent) {
      assertEquals(TreeFillMessage.MessageType.Detect, ((TreeFillMessage) message).type);
    }
  }

  @Test
  void testRaceBetweenIncrementRoundAndDetectFromTheFuture() {
    TreeFillRateLimiter root = new TreeFillRateLimiter(3, 3, 12, ticker, mockMessageDeliverator);
    WindowState window = root.currentWindow();
    window.receive(
        new Detect(1, 3, 2, 2)
    );

    assertFalse(window.selfPermitAllocated);
    assertEquals(1, mockMessageDeliverator.messagesSent.size());
    TreeFillMessage message = (TreeFillMessage) mockMessageDeliverator.messagesSent.get(0);
    assertEquals(TreeFillMessage.MessageType.Acquire, message.type);
    assertEquals(2, ((Acquire)message).getPermitsAcquired());
    assertEquals(1, ((Acquire)message).round);
  }

  private void run12PermitGraph(TreeFillRateLimiter acquiringNode) {
    acquiringNode.acquire();
    acquiringNode.acquire();
    acquiringNode.acquire();
    acquiringNode.acquire();
    acquiringNode.acquire();

    assertTrue(acquiringNode.currentWindow().windowOpen);

    assertEquals(1, acquiringNode.currentWindow().round);

    acquiringNode.acquire();

    assertEquals(2, acquiringNode.currentWindow().round);

    acquiringNode.acquire();
    acquiringNode.acquire();

    assertTrue(acquiringNode.currentWindow().windowOpen);

    acquiringNode.acquire();

    assertEquals(3, acquiringNode.currentWindow().round);

    acquiringNode.acquire();
    acquiringNode.acquire();

    assertTrue(acquiringNode.currentWindow().windowOpen);

    acquiringNode.acquire();

    assertTrue(!acquiringNode.currentWindow().windowOpen);
  }

  class MockMessageDeliverator extends FutureSerialDeliverator {
    public List<Message> messagesSent = new ArrayList<>();
    Map<Long, ClusterRateLimiter> nodesById = new HashMap<>();

    public MockMessageDeliverator() {
      super(new SameThreadExecutorService());
    }

    public void addNode(TreeFillRateLimiter treeFillRateLimiter) {
      nodesById.put(treeFillRateLimiter.getId(), treeFillRateLimiter);
    }

    @Override
    public void send(Message message) {
      messagesSent.add(message);
      ClusterRateLimiter dest = nodesById.get(message.getDst());

      if (dest != null) {
        dest.receive(message);
      }
    }
  }
}
