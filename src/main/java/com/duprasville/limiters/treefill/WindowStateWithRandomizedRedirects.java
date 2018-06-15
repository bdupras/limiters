package com.duprasville.limiters.treefill;

import java.util.Random;

import com.duprasville.limiters.api.MessageSender;
import com.duprasville.limiters.treefill.domain.Detect;

public class WindowStateWithRandomizedRedirects extends WindowState {

  private final Random random;
  private final long LimitForGraphAboveWindowState;

  public WindowStateWithRandomizedRedirects(long id, long N, long W, MessageSender m) {
    super(id, N, W, m);
    random = new Random();
    int row = computeRowOfWindowStateFromId(id);
    LimitForGraphAboveWindowState = (long) Math.pow(2, row);
  }

  // zero indexed row
  private int computeRowOfWindowStateFromId(long id) {
    if (id == 1) return 0;
    int i = 0;
    while ( Math.pow(2,i) <= id ) {
      i++;
    }
    return i-1;
  }

  @Override
  /**
   * If we get here then we know that we are full and our children are full.  So send to a
   * random node higher in the tree
   */
  protected void redirectDetectMessage(Detect detectMesage) {
    long dest = random.nextInt((int) LimitForGraphAboveWindowState) + 1;
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
