package com.duprasville.limiters.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerialExecutorTest {
    @Test
    void execute() throws Exception {
        int MAX = 500;
        CountDownLatch latch = new CountDownLatch(MAX * 2);
        SerialExecutor execOne = new SerialExecutor(ForkJoinPool.commonPool());
        SerialExecutor execTwo = new SerialExecutor(ForkJoinPool.commonPool());
        List<Integer> collectedOne = new ArrayList<>(MAX);
        List<Integer> collectedTwo = new ArrayList<>(MAX);
        for (int i = 0; i < MAX; i++) {
            execOne.execute(collect(collectedOne, i, latch));
            execTwo.execute(collect(collectedTwo, i, latch));
        }
        latch.await();
        List<Integer> expected = IntStream.range(0, MAX).boxed().collect(Collectors.toList());
        assertEquals(expected, collectedOne);
        assertEquals(expected, collectedTwo);
    }

    private Runnable collect(List<Integer> done, int i, CountDownLatch latch) {
        return () -> {
            try {
                if (i % 5 == 0) Thread.sleep(1);
            } catch (InterruptedException ignored) {
            } finally {
                done.add(i);
                latch.countDown();
            }
        };
    }
}
