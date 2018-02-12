package com.duprasville.limiters.treefill;

public class WindowFull extends BaseTreeFillMessage {
    final long permitsAcquired;

    WindowFull(long src, long dst, long permitsAcquired) {
        super(src, dst);
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
                ", permitsAcquired=" + permitsAcquired +
                '}';
    }
}
