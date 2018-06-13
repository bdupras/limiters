package com.duprasville.limiters.treefill.domain;

public class Acquire extends BaseMessage {
    private final long permitsAcquired;

    public Acquire(long src, long dst, long round, long permitsAcquired) {
        super(src, dst, round, MessageType.Acquire);
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

    public long getPermitsAcquired() {
        return this.permitsAcquired;
    }
}
