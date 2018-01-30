package com.duprasville.limiters;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ClusterClusterRateLimitersTest {
    class TestTicker extends Ticker {
        long ticks;
        public long ONE_SECOND = 1_000_000_000L; // nanoseconds

        public TestTicker(long ticks) {
            this.ticks = ticks;
        }

        public long advanceSecs(double seconds) {
            return advance((long) (seconds * ONE_SECOND));
        }

        public long advance(long nanos) {
            return this.ticks += nanos;
        }

        @Override
        public long read() {
            return this.ticks;
        }
    }

    @Test
    void unlimited() {
        assertThat(ClusterRateLimiters.UNLIMITED.tryAcquire(), is(true));
        assertThat(ClusterRateLimiters.UNLIMITED.tryAcquire(Integer.MAX_VALUE), is(true));
    }

    @Test
    void never() {
        assertThat(ClusterRateLimiters.NEVER.tryAcquire(), is(false));
        assertThat(ClusterRateLimiters.NEVER.tryAcquire(Integer.MAX_VALUE), is(false));
    }

    @Test
    void simple() {
        // Guava's limiter has some confusing math that giveth
        // up front and taketh away on the back end
        TestTicker t = new TestTicker(0L);
        ClusterRateLimiter simple = ClusterRateLimiters.createSimple(1L, 1L, 1L, t);
        t.advanceSecs(10.0d);
        assertThat(simple.tryAcquire(1), is(true));
        t.advanceSecs(0.5d);
        assertThat(simple.tryAcquire(1), is(true));
        t.advanceSecs(0.25d);
        assertThat(simple.tryAcquire(1), is(false));
        t.advanceSecs(10.0d);
        assertThat(simple.tryAcquire(1), is(true));
        t.advanceSecs(0.5d);
        assertThat(simple.tryAcquire(1), is(true));
        t.advanceSecs(0.6d);
        assertThat(simple.tryAcquire(1), is(true));
    }

}