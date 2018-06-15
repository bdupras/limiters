package com.duprasville.limiters.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes the submission of tasks to a second executorService.
 */
public class SerialExecutorService extends AbstractExecutorService {
  final BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
  final ExecutorService executor;
  Runnable active;

  public SerialExecutorService(ExecutorService executor) {
    this.executor = executor;
  }

  /**
   * Lock used whenever accessing the state variables
   * (runningTasks, shutdown, terminationCondition) of the executor
   */
  private final Lock lock = new ReentrantLock();
  private final Condition termination = lock.newCondition();

  private volatile boolean flushed = false;
  private volatile boolean shutdown = false;

  public void execute(final Runnable r) {
    lock.lock();
    try {
      if (isShutdown()) {
        throw new RejectedExecutionException("Executor already shutdown");
      }
      executeTask(r);
    } finally {
      lock.unlock();
    }
  }

  private void executeTask(final Runnable r) {
    tasks.add(() -> {
      try {
        r.run();
      } finally {
        scheduleNext();
      }
    });
    if (active == null) {
      scheduleNext();
    }
  }

  private synchronized void scheduleNext() {
    if ((active = tasks.poll()) != null) {
      executor.execute(active);
    }
  }

  @Override
  public boolean isShutdown() {
    lock.lock();
    try {
      return shutdown;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void shutdown() {
    lock.lock();
    try {
      shutdown = true;
      executeTask(() -> flushed = true);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<Runnable> shutdownNow() {
    lock.lock();
    ArrayList<Runnable> remaining;
    try {
      remaining = new ArrayList<>(tasks.size());
      shutdown = true;
      tasks.drainTo(remaining);
      executeTask(() -> flushed = true);
    } finally {
      lock.unlock();
    }
    return remaining;
  }

  @Override
  public boolean isTerminated() {
    lock.lock();
    try {
      return shutdown && flushed;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    long nanos = unit.toNanos(timeout);
    lock.lock();
    try {
      for (; ; ) {
        if (isTerminated()) {
          return true;
        } else if (nanos <= 0) {
          return false;
        } else {
          nanos = termination.awaitNanos(nanos);
        }
      }
    } finally {
      lock.unlock();
    }
  }
}
