package com.duprasville.limiters.treefill.domain;

public class Acquire extends BaseMessage {
    final long round;
    final long permitsAcquired;

    public Acquire(long src, long dst, long window, long round, long permitsAcquired) {
        super(src, dst, window, MessageType.Acquire);
        this.round = round;
        this.permitsAcquired = permitsAcquired;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "src=" + getSrc() +
                ", dst=" + getDst() +
                ", round=" + getRound() +
                ", permitsAcquired=" + permitsAcquired +
                '}';
    }
}
