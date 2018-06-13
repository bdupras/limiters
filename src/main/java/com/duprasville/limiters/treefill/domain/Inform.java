package com.duprasville.limiters.treefill.domain;

public class Inform extends BaseMessage {
    final String msg;
    public Inform(long src, long dst, long window, String msg) {
        super(src, dst, window, MessageType.Inform);
        this.msg = msg;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "src=" + getSrc() +
                ", dst=" + getDst() +
                ", msg='" + msg + '\'' +
                '}';
    }
}
