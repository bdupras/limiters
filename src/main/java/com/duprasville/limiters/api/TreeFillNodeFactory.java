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
   */
  public static Node createNode(MessageDeliverator sender, Executor executor) {
    return null;
  }

}
