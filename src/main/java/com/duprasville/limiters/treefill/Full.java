package com.duprasville.limiters.treefill;

public class Full extends BaseTreeFillMessage {
    final long round;
    final long permitsFilled;

    Full(long src, long dst, long round, long permitsFilled) {
        super(src, dst);
        this.round = round;
        this.permitsFilled = permitsFilled;
    }

    @Override
    public void deliver(TreeFillMessageSink messageSink) {
        messageSink.receive(this);
    }

    @Override
    public String toString() {
        return "Full{" +
                "src=" + src +
                ", dst=" + dst +
                ", round=" + round +
                ", permitsFilled=" + permitsFilled +
                '}';
    }
}
