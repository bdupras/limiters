package com.duprasville.limiters.treefill;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.treefill.domain.Acquire;
import com.duprasville.limiters.treefill.domain.ChildFull;
import com.duprasville.limiters.treefill.domain.CloseWindow;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.treefill.domain.RoundFull;
import com.duprasville.limiters.treefill.domain.TreeFillMessage;
import com.duprasville.limiters.util.SerialExecutor;

class WindowState {
  // Used to receive & throw away very delayed messages - better than mucking with Option<WindowState>
  public static final WindowState NIL_WINDOW =
      new WindowState(1, 1, 1, (message) -> CompletableFuture.completedFuture(null), (e) -> {
      });

  private final Logger logger;
  private final SerialExecutor messageExecutor;

  private final long id;
  private final long parentId;
  private long leftChild = 0;
  private long rightChild = 0;

  private final long N;
  private final long W;
  private int shareThisRound = 0;
  private final boolean hasChildren;
  private boolean[] childPermitsAllocated;

  private MessageDeliverator messageDeliverator;
  private long permitCounter = 0;

  boolean windowOpen = true;
  int round = 1; // begin at one according to the paper
  boolean selfPermitAllocated;
  boolean isThisLastRound;

  private void assertWSuitable(long N, long W) {
    long r = W / N;
    while ((r > 1) && (r % 2 == 0)) {
      r = r >> 1;
    }

    if ((r != 1) || (W % N != 0)) {
      throw new IllegalArgumentException("W must be N * a power of 2");
    }
  }

  public WindowState(long id, long N, long W, MessageDeliverator m, Executor executor) {
    this.id = id;
    this.N = N;

    assertWSuitable(N, W);
    this.W = W;

    this.messageExecutor = new SerialExecutor(executor);

    this.hasChildren = (N > 1) && (this.id <= (N / 2));
    // if we are the root, parentId is 0, which is fine since node ids begin at 1
    this.parentId = id >> 1;

    this.isThisLastRound = false;

    if (this.hasChildren) {
      this.childPermitsAllocated = new boolean[2];
      this.leftChild = id << 1;
      this.rightChild = this.leftChild + 1;
    }

    resetThisNode();
    this.messageDeliverator = m;

    logger = Logger.getLogger(WindowState.class.getSimpleName());
  }

  private void resetThisNode() {
    this.shareThisRound =
        (int) (this.W / ((long) Math.pow(2, this.round) * this.N));

    if (this.shareThisRound == 0) {
      this.isThisLastRound = true;
      this.shareThisRound = 1;
    }

    this.selfPermitAllocated = false;

    if (this.hasChildren) {
      childPermitsAllocated[0] = false;
      childPermitsAllocated[1] = false;
    }

    handleExtraPermitsAcquiredThisRound(this.id);
  }

  public CompletableFuture<Boolean> acquire() {
    return acquire(1);
  }

  public CompletableFuture<Boolean> acquire(long permits) {

    if (this.windowOpen && (this.shareThisRound > 0)) {
      logger.info(
          "WINDOW IS OPEN! " +
              "acquire(1) on node=" + this.id +
              "; shareThisRound=" + this.shareThisRound +
              "; round=" + this.round +
              "; permitCounter=" + this.permitCounter +
              "; isThisLastRound=" + this.isThisLastRound
      );

      messageDeliverator.send(new Acquire(this.id, this.id, this.round, permits));
      return CompletableFuture.completedFuture(true);

    }

    // p + r > w_i:
    // r_1 = w_i - p
    // r_2 = r - r_1
    // acquire(r_1) --> goes to p + r == w_i case
    // acquire(r_2) --> keeps going
    logger.info("cowardly refusing to enqueue permit; RATE LIMIT REACHED!");
    return CompletableFuture.completedFuture(false);
  }

  CompletableFuture<Void> receive(TreeFillMessage message) {
    messageExecutor.execute(() -> process(message));
    return CompletableFuture.completedFuture(null);
  }

  CompletableFuture<Void> process(TreeFillMessage message) {
    logger.info("receive(): " + message.toString());

    boolean areChildrenFull = isGraphBelowFull();
    boolean amRoot = isRoot();

    switch (message.type) {

      case Acquire:
        long permits = ((Acquire) message).getPermitsAcquired();

        if (this.permitCounter + permits <= this.W) {
          this.permitCounter += permits;
          if (this.permitCounter >= this.shareThisRound) {
            handleExtraPermitsAcquiredThisRound(this.id);
          }
        }

        break;

      case Detect:
        if (!this.selfPermitAllocated) {
          this.selfPermitAllocated = true;

          if (isGraphBelowFull()) {
            notifyParentIfAny((Detect)message);
          }
        } else if (!areChildrenFull) {
          Optional<Long> maybeUnfilledChild = getUnfilledChild();

          if (maybeUnfilledChild.isPresent()) {
            messageDeliverator.send(
                new Detect(
                    message.getSrc(),
                    maybeUnfilledChild.get(),
                    this.round,
                    ((Detect) message).getPermitsAcquired()
                )
            );
          }
        } else if (!amRoot) {
          messageDeliverator.send(
              new Detect(
                  message.getSrc(),
                  this.parentId,
                  this.round,
                  ((Detect) message).getPermitsAcquired()
              )
          );
        } else {
          if (isGraphBelowFull()) saveUnrecordedDetects((Detect)message);
        }
        break;

      case RoundFull:
        // either we are in the last round and are the root
        // so we send ourselves a closeWindow, OR
        // we advance to the next round
        if (amRoot && this.isThisLastRound) {
          messageDeliverator.send(
              new CloseWindow(
                  this.id,
                  this.id,
                  this.round
              )
          );
        } else {
          advanceRound();
        }
        break;

      case CloseWindow:
        this.windowOpen = false;

        if (this.hasChildren) {
          messageDeliverator.send(
              new CloseWindow(
                  this.id,
                  this.leftChild,
                  this.round
              )
          );

          messageDeliverator.send(
              new CloseWindow(
                  this.id,
                  this.rightChild,
                  this.round
              )
          );
        }
        break;

      case ChildFull:
        long idOfFullChild = message.getSrc();

        if (this.leftChild == idOfFullChild) {
          this.childPermitsAllocated[0] = true;
        } else if (this.rightChild == idOfFullChild) {
          this.childPermitsAllocated[1] = true;
        } else if (this.id != idOfFullChild) {
          throw new RuntimeException("id " + idOfFullChild +
              " is not currently a child of " + this.id);
        }

        // we have just changed our view of the graph below's state
        if (isGraphBelowFull()) {
          if (!amRoot) {
            messageDeliverator.send(
                new ChildFull(
                    this.id,
                    this.parentId,
                    this.round
                )
            );
          } else {
            messageDeliverator.send(
                new RoundFull(
                    this.id,
                    this.id,
                    this.round
                )
            );
          }
        }
        break;

      default:
        throw new UnsupportedOperationException("oops");
    }

    return CompletableFuture.completedFuture(null);
  }

  private void handleExtraPermitsAcquiredThisRound(long messageSrc) {
    while (this.permitCounter >= this.shareThisRound) {

      // we need to allow more collection of permits locally
      this.permitCounter -= this.shareThisRound;

      messageDeliverator.send(
          new Detect(
              messageSrc,
              this.id,
              this.round,
              this.shareThisRound
          )
      );
    }
  }

  private boolean isRoot() {
    return this.id == 1;
  }

  private void advanceRound() {
    this.round++;
    resetThisNode();

    if (this.hasChildren) {
      messageDeliverator.send(
          new RoundFull(
              this.id,
              this.leftChild,
              this.round
          )
      );

      messageDeliverator.send(
          new RoundFull(
              this.id,
              this.rightChild,
              this.round
          )
      );
    }
  }

  private Optional<Long> getUnfilledChild() {
    boolean graphBelowIsFull = isGraphBelowFull();

    if (!graphBelowIsFull && !childPermitsAllocated[0]) {
      return Optional.of(this.leftChild);
    } else if (!graphBelowIsFull && !childPermitsAllocated[1]) {
      return Optional.of(this.rightChild);
    }

    return Optional.empty();
  }

  private boolean isGraphBelowFull() {
    if (!this.hasChildren) {
      return this.selfPermitAllocated;
    } else {
      for (boolean permit : childPermitsAllocated) {
        if (!permit) return false;
      }

      return true;
    }
  }

  /**
   * This should only be called when the node is the root.  There is a detect, but no place to store it.
   */
  private void saveUnrecordedDetects(Detect message) {
    assert(isRoot());
    permitCounter += message.getPermitsAcquired();
  }

  private void notifyParentIfAny(Detect message) {
    if (!isRoot()) {
      messageDeliverator.send(
          new ChildFull(this.id, this.parentId, this.round)
      );
    } else if (message.getSrc() == this.id) {
      messageDeliverator.send(
          new RoundFull(
              this.id,
              this.id,
              this.round
          )
      );
    }
  }
}
