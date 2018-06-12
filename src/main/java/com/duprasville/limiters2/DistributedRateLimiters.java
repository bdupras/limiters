package com.duprasville.limiters2;

import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ForkedRateLimiter;

import static com.duprasville.limiters2.util.Utils.spread;

public class DistributedRateLimiters {
    public static final DistributedRateLimiter UNLIMITED = (permits) -> true;
    public static final DistributedRateLimiter NEVER = (permits) -> false;
    public static final Ticker SYSTEM_TICKER = Ticker.systemTicker();

    public static DistributedRateLimiter createPerShard(long permitsPerSecond, long clusterShard, long clusterSize, Ticker ticker) {
        long shardShare = spread(clusterShard, permitsPerSecond, clusterSize);

        return new DistributedRateLimiter() {
            private ForkedRateLimiter underlying = ForkedRateLimiter.create(shardShare, ticker);

            @Override
            public boolean tryAcquire(long permitsPerSecond) {
                return permitsPerSecond <= Integer.MAX_VALUE && underlying.tryAcquire((int) permitsPerSecond);
            }

            @Override
            public void setRate(long permitsPerSecond) {
                underlying.setRate(permitsPerSecond);
            }
        };
    }

    public static DistributedRateLimiter createPerShard(long permitsPerSecond, long clusterShard, long clusterSize) {
        return createPerShard(clusterSize, clusterShard, permitsPerSecond, SYSTEM_TICKER);
    }
}
