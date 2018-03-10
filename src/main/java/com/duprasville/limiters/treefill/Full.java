package com.duprasville.limiters.treefill;

public class Full extends BaseTreeFillMessage {
    public final long round;
    public final long permitsAcquired;

    Full(long src, long dst, long window, long round, long permitsAcquired) {
        super(src, dst, window);
        this.round = round;
        this.permitsAcquired = permitsAcquired;
    }

    @Override
    public void deliver(TreeFillMessageSink messageSink) {
        messageSink.receive(this);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "src=" + src +
                ", dst=" + dst +
                ", window=" + window +
                ", round=" + round +
                ", permitsAcquired=" + permitsAcquired +
                '}';
    }
}
