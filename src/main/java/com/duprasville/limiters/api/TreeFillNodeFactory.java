package com.duprasville.limiters.api;

import java.util.concurrent.Executor;

/**
 * This 'should' be the ONLY dependency on implementation classes keeping the library impl separate
 * from the api.  It's only dependency is 'construction'
 */
public class TreeFillNodeFactory {

  /**
   * To be added to
   *
   * Definitely need some 'Time' element for tests to be included here.  Are they using
   */
  public static DistributedRateLimiter createNode(MessageDeliverator sender, Executor executor, NodeConfig nodeConfig) {
    return null;
  }

}
