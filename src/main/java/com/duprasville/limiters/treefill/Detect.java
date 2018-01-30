package com.duprasville.limiters.treefill;

public class Detect extends BaseTreeFillMessage {
    final long round;
    final long permitsAcquired;

    Detect(long src, long dst, long round, long permitsAcquired) {
        super(src, dst);
        this.round = round;
        this.permitsAcquired = permitsAcquired;
    }

    @Override
    public void deliver(TreeFillMessageSink messageSink) {
        messageSink.receive(this);
    }

    @Override
    public String toString() {
        return "Detect{" +
                "src=" + src +
                ", dst=" + dst +
                ", round=" + round +
                ", permitsAcquired=" + permitsAcquired +
                '}';
    }
}
