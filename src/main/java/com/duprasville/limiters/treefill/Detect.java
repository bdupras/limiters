package com.duprasville.limiters.treefill;

public class Detect extends BaseTreeFillMessage {
    final long round;
    final long permitsAcquired;

    Detect(long src, long dst, long window, long round, long permitsAcquired) {
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
                ", round=" + round +
                ", permitsAcquired=" + permitsAcquired +
                '}';
    }
}
