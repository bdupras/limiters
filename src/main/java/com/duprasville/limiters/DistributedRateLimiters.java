package com.duprasville.limiters;

public class DistributedRateLimiters {
    public static final DistributedRateLimiter UNLIMITED = (permits) -> true;
    public static final DistributedRateLimiter NEVER = (permits) -> false;
}
