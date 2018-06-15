package com.duprasville.limiters.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerialExecutorServiceTest {
  @Test
  void execute() throws Exception {
    int MAX = 500;
    List<Integer> expected = IntStream.range(0, MAX).boxed().collect(Collectors.toList());

    SerialExecutorService execOne = new SerialExecutorService(ForkJoinPool.commonPool());
    SerialExecutorService execTwo = new SerialExecutorService(ForkJoinPool.commonPool());
    List<Integer> collectedOne = new ArrayList<>(MAX);
    List<Integer> collectedTwo = new ArrayList<>(MAX);
    CountDownLatch latchOne = new CountDownLatch(1);
    CountDownLatch latchTwo = new CountDownLatch(1);

    for (int i = 0; i < MAX; i++) {
      execOne.execute(collect(collectedOne, i));
      execTwo.execute(collect(collectedTwo, i));
    }

    execOne.execute(latchOne::countDown);
    latchOne.await();
    assertFalse(execOne.isShutdown());
    assertFalse(execOne.isTerminated());
    execOne.shutdown();

    execOne.awaitTermination(2, SECONDS);
    assertTrue(execOne.isShutdown());
    assertTrue(execOne.isTerminated());

    assertEquals(expected, collectedOne);

    execTwo.execute(latchTwo::countDown);
    latchTwo.await();
    assertEquals(expected, collectedTwo);
  }

  private Runnable collect(List<Integer> done, int i) {
    return () -> {
      try {
        if (i % 5 == 0) Thread.sleep(1);
      } catch (InterruptedException ignored) {
      } finally {
        done.add(i);
      }
    };
  }
}
