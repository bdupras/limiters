package com.duprasville.limiters.treefill;

public class WindowFull extends BaseTreeFillMessage {
    public final long permitsAcquired;

    WindowFull(long src, long dst, long window, long permitsAcquired) {
        super(src, dst, window);
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
                ", permitsAcquired=" + permitsAcquired +
                '}';
    }
}
