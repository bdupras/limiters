package com.duprasville.limiters2.treefill2;

abstract class BaseMessage implements Message {
    public final long window;
    public final long src;
    public final long dst;

    @Override
    public long getSrc() {
        return src;
    }

    @Override
    public long getDst() {
        return dst;
    }

    @Override
    public long getWindow() {
        return window;
    }

    BaseMessage(long src, long dst, long window) {
        this.src = src;
        this.dst = dst;
        this.window = window;
    }
}
