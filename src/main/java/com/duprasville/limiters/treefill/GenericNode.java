package com.duprasville.limiters.treefill;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.api.DistributedRateLimiter;
import com.duprasville.limiters.treefill.domain.ChildFull;
import com.duprasville.limiters.treefill.domain.CloseWindow;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.treefill.domain.RoundFull;

public class GenericNode implements DistributedRateLimiter {
  private final Logger logger;

  private final long id;
  private final long parentId;
  private long leftChild = 0;
  private long rightChild = 0;

  private final int N;
  private final long W;
  private int shareThisRound = 0;
  private final boolean hasChildren;
  private boolean[] childPermitsAllocated;

  private MessageDeliverator messageDeliverator;
  private long permitCounter = 0;

  boolean windowOpen = true;
  int round = 1; // begin at one
  boolean selfPermitAllocated;

  GenericNode(int id, int N, int W, boolean hasChildren, MessageDeliverator m) {
    this.id = id;
    this.N = N;
    this.W = W;
    this.hasChildren = hasChildren;
    // if we are the root, parentId is 0, which is fine since node ids begin at 1
    this.parentId = id >> 1;

    if (this.hasChildren) {
      this.childPermitsAllocated = new boolean[2];
      this.leftChild = id << 1;
      this.rightChild = this.leftChild + 1;
    }

    resetThisNode();
    this.messageDeliverator = m;

    logger = Logger.getLogger(GenericNode.class.getSimpleName());
  }

  private void resetThisNode() {
    this.shareThisRound =
        (int) (this.W / (((long) Math.pow(2, Math.abs(this.round - 1))) * this.N));

    this.selfPermitAllocated = false;
    this.permitCounter = 0;

    if (this.hasChildren) {
      childPermitsAllocated[0] = false;
      childPermitsAllocated[1] = false;
    }
  }

  @Override
  public Long getId() {
    return this.id;
  }

  @Override
  public CompletableFuture<Boolean> acquire() {
    if (this.windowOpen) {
      logger.info(
          "WINDOW IS OPEN! " +
              "acquire(1) on node=" + this.id +
              "; shareThisRound=" + this.shareThisRound +
              "; round=" + this.round +
              "; permitCounter=" + this.permitCounter
      );

      // enqueue(Detect d) -- on ourselves
      if ((this.permitCounter + 1) <= this.shareThisRound) {
        this.permitCounter++;
        handleDetectMessage(new Detect(this.id, this.id, this.round, this.permitCounter), isRoot(), isGraphBelowFull());
        return CompletableFuture.completedFuture(true);
      } else if (((this.permitCounter + 1) > this.shareThisRound) &&
          ((this.permitCounter + 1) <= this.W) &&
          (this.N > this.W)) {
        // we can potentially reshuffle
        messageDeliverator.send(
            new Detect(
                this.id,
                this.parentId,
                this.round,
                1 // rebalancing the extra permit
            )
        );
        return CompletableFuture.completedFuture(true);
      } else {
        logger.info("too many messages; RATE LIMIT REACHED!");
        if (this.windowOpen) {
          messageDeliverator.send(
              new RoundFull(
                  this.id,
                  this.id,
                  this.round
              )
          );
        }
        return CompletableFuture.completedFuture(false);
      }
    }

    // p + r > w_i:
    // r_1 = w_i - p
    // r_2 = r - r_1
    // acquire(r_1) --> goes to p + r == w_i case
    // acquire(r_2) --> keeps going
    logger.info("cowardly refusing to enqueue permit; RATE LIMIT REACHED!");
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public CompletableFuture<Void> receive(Message message) {
    logger.info("receive(): " + message.toString());

    boolean areChildrenFull = isGraphBelowFull();
    boolean amRoot = isRoot();

    switch (message.getType()) {
      case Detect:
        handleDetectMessage((Detect) message, amRoot, areChildrenFull);
        break;
      case RoundFull:
        // either we are in the last round and are the root
        // so we send ourselves a closeWindow, OR
        // we advance to the next round
        if (amRoot && (this.permitCounter == this.shareThisRound)) {
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

        if (areChildrenFull) {
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

  private void handleDetectMessage(Detect message, boolean amRoot, boolean graphBelowIsFull) {
    if (!this.selfPermitAllocated && !this.hasChildren) {
      this.selfPermitAllocated = true;
      notifyParentOrIncrementGlobalRound();
    } else if (!graphBelowIsFull) {
      Optional<Long> maybeUnfilledChild = getUnfilledChild();

      if (maybeUnfilledChild.isPresent()) {
        messageDeliverator.send(
            new Detect(
                message.getSrc(),
                maybeUnfilledChild.get(),
                this.round,
                message.getPermitsAcquired()
            )
        );
      }
    } else if (!amRoot) {
      messageDeliverator.send(
          new Detect(
              message.getSrc(),
              this.parentId,
              this.round,
              message.getPermitsAcquired()
          )
      );
    } else /*if (amRoot && graphBelowIsFull)*/ {
      messageDeliverator.send(
          new RoundFull(
              this.id,
              this.id,
              this.round
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

  private void notifyParentOrIncrementGlobalRound() {
    if (!isRoot()) {
      messageDeliverator.send(
          new ChildFull(this.id, this.parentId, this.round)
      );
    } else { // we are the root
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
