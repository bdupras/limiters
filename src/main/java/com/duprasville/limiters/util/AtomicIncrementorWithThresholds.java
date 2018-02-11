package com.duprasville.limiters.util;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;

public class AtomicIncrementorWithThresholds {
    private final AtomicLong underlying;
    private final List<Long> thresholds;
    private final Consumer<Long> callback;
    private final AtomicLong lastThreshold;

    public AtomicIncrementorWithThresholds(long initialValue, List<Long> thresholds, Consumer<Long> callback) {
        this.underlying = new AtomicLong(initialValue);
        this.lastThreshold = new AtomicLong(initialValue);
        this.thresholds = thresholds.stream().sorted().distinct().collect(toList());
        this.callback = callback;
    }

    private long ensureThreshold(long value) {
        thresholds.stream().filter(t -> t <= value).max(naturalOrder()).ifPresent(ensured -> {
//        thresholds.stream().filter(t -> t <= value).forEachOrdered(ensured -> {
            long last = lastThreshold.get();
            if (last < ensured) {
                if (lastThreshold.compareAndSet(last, ensured)) {
                    callback.accept(ensured);
                }
            }
        });
        return value;
    }

    public long get() {
        return underlying.get();
    }

    public long addAndGet(long delta) {
        if (delta < 0L) throw new UnsupportedOperationException("delta must be positive");
        long ret = underlying.addAndGet(delta);
        ensureThreshold(ret);
        return ret;
    }

    public long getAndAdd(long delta) {
        if (delta < 0L) throw new UnsupportedOperationException("delta must be positive");
        long ret = underlying.getAndAdd(delta);
        ensureThreshold(get());
        return ret;
    }
}
