package com.duprasville.limiters.treefill;

interface Message {
    void deliver(long src, long dst, TreeFillRateLimiter treeFillRateLimiter);
}

class Inform implements Message {
    final String msg;
    Inform(String msg) {
        this.msg = msg;
    }

    @Override
    public void deliver(long src, long dst, TreeFillRateLimiter treeFillRateLimiter) {
        treeFillRateLimiter.receiveInform(src, dst, this);
    }
}
