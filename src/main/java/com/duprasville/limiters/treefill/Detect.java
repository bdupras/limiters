package com.duprasville.limiters.treefill;

public class Detect extends BaseMessage {
    final long round;
    final long permitsAcquired;

    Detect(long src, long dst, long window, long round, long permitsAcquired) {
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
