package com.duprasville.limiters.treefill;

import java.util.Random;

import com.duprasville.limiters.api.MessageSender;
import com.duprasville.limiters.treefill.domain.Detect;

// it's worth keeping this separate from WindowState until we can run some # of message
// comparisons and record them. Then,
// TODO merge this into WindowState (we should always redirect Detects to a random member
// of the row above us, not just our parent, as it takes far fewer messages to do so.
public class WindowStateWithRandomizedRedirects extends WindowState {

  private final Random random;
  private final long limitForGraphAboveWindowState;

  public WindowStateWithRandomizedRedirects(long id, long N, long W, MessageSender m) {
    super(id, N, W, m);
    random = new Random();
    int row = computeRowOfWindowStateFromId(id);
    limitForGraphAboveWindowState = (long) Math.pow(2, row);
  }

  // zero indexed row
  private int computeRowOfWindowStateFromId(long id) {
    if (id == 1) return 0;
    int i = 0;
    while (Math.pow(2, i) <= id) {
      i++;
    }
    return i - 1;
  }

  @Override
  /**
   * If we get here then we know that we are full and our children are full.  So send to a
   * random node higher in the tree
   */
  protected void redirectDetectMessage(Detect detectMesage) {
    long dest = random.nextInt((int) limitForGraphAboveWindowState) + 1;
    messageSender.send(  // Sending to parent
        new Detect(
            detectMesage.getSrc(),
            dest,
            this.round,
            detectMesage.getPermitsAcquired()
        )
    );
  }
}
