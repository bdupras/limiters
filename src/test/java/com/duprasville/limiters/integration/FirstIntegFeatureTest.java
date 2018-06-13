package com.duprasville.limiters.integration;

import java.util.concurrent.ExecutionException;

import com.duprasville.limiters.api.Node;
import com.duprasville.limiters.api.TreeFillNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class FirstIntegFeatureTest {

  @BeforeEach
  void init() {
    Node treeNode = TreeFillNodeFactory.createNode(null, null);
  }

  @Test
  void testAcquire() throws ExecutionException, InterruptedException {
  }

}
