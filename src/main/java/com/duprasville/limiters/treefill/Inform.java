package com.duprasville.limiters.treefill;

public class Inform extends BaseTreeFillMessage {
    public final String msg;
    Inform(long src, long dst, long window, String msg) {
        super(src, dst, window);
        this.msg = msg;
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
                ", msg='" + msg + '\'' +
                '}';
    }
}
