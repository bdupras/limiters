package com.duprasville.limiters;

import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ForkedRateLimiter;

import static com.duprasville.limiters.util.Utils.spread;

public class RateLimiters {
    public static final RateLimiter UNLIMITED = (permits) -> true;
    public static final RateLimiter NEVER = (permits) -> false;
    public static final Ticker SYSTEM_TICKER = Ticker.systemTicker();

    public static RateLimiter createSimple(long permitsPerSecond, Ticker ticker) {
        return new RateLimiter() {
            private ForkedRateLimiter underlying = ForkedRateLimiter.create(permitsPerSecond, ticker);

            @Override
            public boolean tryAcquire(int permits) {
                return underlying.tryAcquire(permits);
            }

            @Override
            public void setRate(long permitsPerSecond) {
                underlying.setRate(permitsPerSecond);
            }
        };
    }

    public static RateLimiter createSimple(long permitsPerSecond) {
        return createSimple(permitsPerSecond, SYSTEM_TICKER);
    }

    public static RateLimiter createPerClusterNode(long clusterSize, long clusterShard, long permitsPerSecond, Ticker ticker) {
        long myShare = spread(clusterShard, permitsPerSecond, clusterSize);
        return createSimple(myShare, ticker);
    }

    public static RateLimiter createPerClusterNode(long clusterSize, long clusterShard, long permitsPerSecond) {
        return createPerClusterNode(clusterSize, clusterShard, permitsPerSecond, SYSTEM_TICKER);
    }
}