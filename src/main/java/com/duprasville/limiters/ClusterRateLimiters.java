package com.duprasville.limiters;

import com.duprasville.limiters.util.Utils;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ForkedRateLimiter;

import static com.duprasville.limiters.util.Utils.spread;

public class ClusterRateLimiters {
    public static final ClusterRateLimiter UNLIMITED = (permits) -> true;
    public static final ClusterRateLimiter NEVER = (permits) -> false;
    public static final Ticker SYSTEM_TICKER = Ticker.systemTicker();

    public static ClusterRateLimiter createSimple(long permitsPerSecond, long clusterShard, long clusterSize, Ticker ticker) {
        long shardShare = Utils.spread(clusterShard, permitsPerSecond, clusterSize);

        return new ClusterRateLimiter() {
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

    public static ClusterRateLimiter createSimple(long permitsPerSecond, long clusterShard, long clusterSize) {
        return createSimple(permitsPerSecond, clusterShard, clusterSize, SYSTEM_TICKER);
    }

    public static ClusterRateLimiter createPerClusterNode(long permitsPerSecond, long clusterShard, long clusterSize, Ticker ticker) {
        return createSimple(permitsPerSecond, clusterShard, clusterSize, ticker);
    }

    public static ClusterRateLimiter createPerClusterNode(long permitsPerSecond, long clusterShard, long clusterSize) {
        return createPerClusterNode(clusterSize, clusterShard, permitsPerSecond, SYSTEM_TICKER);
    }
}