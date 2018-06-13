package com.duprasville.limiters.treefill.domain;

public class ChildFull extends BaseMessage {
    private final long round;

    public ChildFull(long src, long dst, long round) {
        super(src, dst, round, MessageType.ChildFull);
        this.round = round;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "src=" + getSrc() +
                ", dst=" + getDst() +
                ", round=" + getRound() +
                '}';
    }
}
