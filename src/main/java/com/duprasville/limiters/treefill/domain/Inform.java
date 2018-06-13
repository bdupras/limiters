package com.duprasville.limiters.treefill.domain;

public class Inform extends TreeFillMessage {
    final String msg;
    public Inform(long src, long dst, long round, String msg) {
        super(src, dst, round, MessageType.Inform);
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
