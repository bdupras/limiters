package com.duprasville.limiters.treefill;

public class Inform extends BaseTreeFillMessage {
    final String msg;
    Inform(long src, long dst, String msg) {
        super(src, dst);
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
                ", msg='" + msg + '\'' +
                '}';
    }
}
