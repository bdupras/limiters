package com.duprasville.limiters.treefill.domain;

import com.duprasville.limiters.api.Message;

public class ChildFull extends Message {
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
                ", round=" + round +
                '}';
    }
}
