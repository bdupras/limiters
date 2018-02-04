package com.duprasville.limiters.util;

import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicToggle {
    private final AtomicBoolean bool = new AtomicBoolean();

    public AtomicToggle(boolean initialValue) {
        this.bool.set(initialValue);
    }

    public boolean trySet(boolean value) {
        return bool.compareAndSet(!value, value);
    }
}
