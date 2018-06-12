package com.duprasville.limiters2.treefill2;

public class Acquire extends BaseMessage {
    final long round;
    final long permitsAcquired;

    Acquire(long src, long dst, long window, long round, long permitsAcquired) {
        super(src, dst, window);
        this.round = round;
        this.permitsAcquired = permitsAcquired;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "src=" + src +
                ", dst=" + dst +
                ", round=" + round +
                ", permitsAcquired=" + permitsAcquired +
                '}';
    }
}
