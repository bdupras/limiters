package com.duprasville.limiters.treefill;

public class Inform extends BaseMessage {
    final String msg;
    Inform(long src, long dst, long window, String msg) {
        super(src, dst, window);
        this.msg = msg;
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
