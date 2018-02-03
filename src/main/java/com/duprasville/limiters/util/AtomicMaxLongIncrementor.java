package com.duprasville.limiters.util;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicMaxLongIncrementor {
    private final long maxValueInclusive;
    private final AtomicLong current;

    public AtomicMaxLongIncrementor(long initialValue, long maxValueInclusive) {
        this.current = new AtomicLong(initialValue);
        this.maxValueInclusive = maxValueInclusive;
    }

    public long get() {
        return current.get();
    }

    /**
     * Attempts to increment up to a maximum value.
     * @param delta amount to increment, must be positive and > 0
     * @return positive current value if increment was successful, negative of current value if increment failed
     */
    public boolean tryIncrement(long delta) {
        if (delta <= 0)
            throw new IllegalArgumentException("cowardly refusing to increment by 0 or less");
        long c = current.get();
        long cx = c + delta;
        while (cx <= maxValueInclusive) {
            if (current.compareAndSet(c, cx)) {
                return true;
            }
            c = current.get();
            cx = c + delta;
        }
        return false;
    }

    /**
     * Attempts to increment up to a maximum value.
     * @param delta amount to increment, must be positive and > 0
     * @return positive current value if increment was successful, negative of current value if increment failed
     */
    public boolean tryDecrement(long delta) {
        if (delta <= 0)
            throw new IllegalArgumentException("cowardly refusing to decrement by 0 or less0");
        long c = current.get();
        long cx = c - delta;
        while (cx >= 0) {
            if (current.compareAndSet(c, cx)) {
                return true;
            }
            c = current.get();
            cx = c - delta;
        }
        return false;
    }
}
