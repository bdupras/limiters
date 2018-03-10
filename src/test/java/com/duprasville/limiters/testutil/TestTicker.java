package com.duprasville.limiters.testutil;

import com.google.common.base.Ticker;

public class TestTicker extends Ticker {
    private long ticks;

    public TestTicker() {
        this(0L);
    }

    public TestTicker(long ticks) {
        this.ticks = ticks;
    }

    public long advanceSecs(double seconds) {
        long ONE_SECOND = 1_000_000_000L;
        return advanceNanos((long) (seconds * ONE_SECOND));
    }

    public long advanceNanos(long nanos) {
        return this.ticks += nanos;
    }

    @Override
    public long read() {
        return this.ticks;
    }
}
