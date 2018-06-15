package com.duprasville.limiters.treefill;

import java.util.Random;

import com.duprasville.limiters.api.MessageSender;
import com.duprasville.limiters.treefill.domain.Detect;

public class WindowStateWithRandomizedRedirects extends WindowState {

  private final Random random;

  public WindowStateWithRandomizedRedirects(long id, long N, long W, MessageSender m) {
    super(id, N, W, m);
    random = new Random();
  }

  @Override
  /**
   * If we get here then we know that we are full and our children are full.  So send to a
   * random node higher in the tree
   */
  protected void redirectDetectMessage(Detect detectMesage) {
    long dest = random.nextInt((int) this.id-1) + 1;
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
