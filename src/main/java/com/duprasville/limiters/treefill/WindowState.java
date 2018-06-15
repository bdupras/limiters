package com.duprasville.limiters.treefill;

import java.util.logging.Logger;

import com.duprasville.limiters.api.MessageSender;
import com.duprasville.limiters.treefill.domain.Acquire;
import com.duprasville.limiters.treefill.domain.ChildFull;
import com.duprasville.limiters.treefill.domain.CloseWindow;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.treefill.domain.RoundFull;
import com.duprasville.limiters.treefill.domain.TreeFillMessage;

class WindowState {
  // Used to receive & throw away very delayed messages - better than mucking with Option<WindowState>
  public static final WindowState NIL = new WindowState(1, 1, 1, MessageSender.NIL);

  private final Logger logger;

  final long id;
  final long parentId;
  long leftChild = 0;
  long rightChild = 0;

  final long N;
  final long W;
  int shareThisRound = 0;
  final boolean hasChildren;
  boolean[] childPermitsAllocated;

  MessageSender messageSender;
  private long permitCounter = 0;
  private long knownPermitsAcquiredAcrossWholeCluster = 0;

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

  public WindowState(long id, long N, long W, MessageSender m) {
    this.id = id;
    this.N = N;

    assertWSuitable(N, W);
    this.W = W;

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
    this.messageSender = m;

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

  public boolean acquire() {
    return acquire(1);
  }

  public boolean acquire(long permits) {

    if ((this.permitCounter + permits + this.knownPermitsAcquiredAcrossWholeCluster) > this.W) {
      return false;
    }

    if (this.windowOpen && (this.shareThisRound > 0)) {
      logger.info(
          "WINDOW IS OPEN! " +
              "acquire(1) on node=" + this.id +
              "; shareThisRound=" + this.shareThisRound +
              "; round=" + this.round +
              "; permitCounter=" + this.permitCounter +
              "; isThisLastRound=" + this.isThisLastRound
      );

      messageSender.send(new Acquire(this.id, this.id, this.round, permits));
      return true;

    }

    // p + r > w_i:
    // r_1 = w_i - p
    // r_2 = r - r_1
    // acquire(r_1) --> goes to p + r == w_i case
    // acquire(r_2) --> keeps going
    logger.info("cowardly refusing to enqueue permit; RATE LIMIT REACHED!");
    return false;
  }

  void receive(TreeFillMessage message) {
    if (message.round != this.round) {
      logger.info("Node " + this.id + " received a message from round " + message.round
          + " while in round " + this.round);
    }

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
        Detect detectMesage = (Detect) message;
        if (detectMesage.getPermitsAcquired() > this.shareThisRound) {
          splitAndResendDetectFromAPastRound(detectMesage);
        } else if (detectMesage.round > this.round) {
          resendDetectFromAFutureRoundAsAcquire(detectMesage);
        } else {
          if (!this.selfPermitAllocated) {
            this.selfPermitAllocated = true;

            if (isGraphBelowFull()) {
              notifyParentIfAny(detectMesage);
            }
          } else if (!areChildrenFull) {
            long unfilledChild = getUnfilledChild();

            messageSender.send(
                new Detect(
                    message.getSrc(),
                    unfilledChild,
                    this.round,
                    detectMesage.getPermitsAcquired()
                )
            );
          } else if (!amRoot) {
            redirectDetectMessage(detectMesage);
          } else /* graph is full and I am Root */ {
            saveUnrecordedDetects(detectMesage);
          }
        }
        break;

      case RoundFull:
        // either we are in the last round and are the root
        // so we send ourselves a closeWindow, OR
        // we advance to the next round
        if (amRoot && this.isThisLastRound) {
          messageSender.send(
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
          messageSender.send(
              new CloseWindow(
                  this.id,
                  this.leftChild,
                  this.round
              )
          );

          messageSender.send(
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
            messageSender.send(
                new ChildFull(
                    this.id,
                    this.parentId,
                    this.round
                )
            );
          } else {
            messageSender.send(
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
  }

  /**
   * When a WindowState is full, then DetectMessages should be resent to another WindowState.
   *
   * To try different strategies for the resend, override this method
   */
  protected void redirectDetectMessage(Detect detectMesage) {
    messageSender.send(  // Sending to parent
        new Detect(
            detectMesage.getSrc(),
            this.parentId,
            this.round,
            detectMesage.getPermitsAcquired()
        )
    );
  }

  private void resendDetectFromAFutureRoundAsAcquire(Detect message) {
    messageSender.send(
        new Acquire(
            message.getSrc(),
            this.id,
            this.round,
            message.getPermitsAcquired()
        )
    );
  }

  private void splitAndResendDetectFromAPastRound(Detect message) {
    long permits = message.getPermitsAcquired();
    while (permits >= this.shareThisRound) {
      messageSender.send(
          new Detect(
              message.getSrc(),
              this.id,
              this.round,
              // if we are interested in checking how many of these happen later,
              // we could add an originator round field
              this.shareThisRound
          )
      );
      permits -= this.shareThisRound;
    }
  }

  private void handleExtraPermitsAcquiredThisRound(long messageSrc) {
    while (this.permitCounter >= this.shareThisRound) {

      // we need to allow more collection of permits locally
      this.permitCounter -= this.shareThisRound;

      messageSender.send(
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

    this.knownPermitsAcquiredAcrossWholeCluster += this.N * this.shareThisRound;

    this.round++;
    resetThisNode();

    if (this.hasChildren) {
      messageSender.send(
          new RoundFull(
              this.id,
              this.leftChild,
              this.round
          )
      );

      messageSender.send(
          new RoundFull(
              this.id,
              this.rightChild,
              this.round
          )
      );
    }
  }

  /**
   * Only call this when you know there is an unfilled child.
   */
  private Long getUnfilledChild() {
    if (isGraphBelowFull()) {
      throw new RuntimeException("You do not have children available to pass permits to!");
    }

    if (!childPermitsAllocated[0]) {
      return this.leftChild;
    } else {
      return this.rightChild;
    }
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
    if (isRoot()) {
      permitCounter += message.getPermitsAcquired();
    } else {
      logger.info("WARNING: We weren't the root node, but we were told to " +
          "save unrecorded Detects -- this is a faulty state!");
    }
  }

  private void notifyParentIfAny(Detect message) {
    if (!isRoot()) {
      messageSender.send(
          new ChildFull(this.id, this.parentId, this.round)
      );
    } else if (message.getSrc() == this.id) {
      messageSender.send(
          new RoundFull(
              this.id,
              this.id,
              this.round
          )
      );
    }
  }
}
