package com.duprasville.limiters.util;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicMaxLongIncrementor {
    private final long max;
    private final AtomicLong current;

    public AtomicMaxLongIncrementor(long initialValue, long max) {
        this.current = new AtomicLong(initialValue);
        this.max = max;
    }

    public long get() {
        return current.get();
    }

    /**
     * Attempts to increment up to a maximum value.
     * @param x amount to increment
     * @return positive current value if increment was successful, negative of current value if increment failed
     */
    public long tryIncrement(long x) {
        long c = current.get();
        while ((c + x) <= max) {
            if (current.compareAndSet(c, c+x)) {
                return (c+x);
            }
            c = current.get();
        }
        return -c;
    }
}
