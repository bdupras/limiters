package com.duprasville.limiters.comms;

public abstract class BaseMessage implements Message {
    public final long src;
    public final long dst;

    public BaseMessage(long src, long dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public long getSrc() {
        return src;
    }

    @Override
    public long getDst() {
        return dst;
    }
}
