package com.duprasville.limiters.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class AtomicMap<K, V> {
    private final ConcurrentHashMap<K, AtomicReference<V>> map = new ConcurrentHashMap<>();
    private Supplier<V> initializer = () -> null;

    public AtomicMap() {
    }

    public AtomicMap(Supplier<V> initializer) {
        this();
        this.initializer = initializer;
    }

    public V get(K key) {
        return at(key).get();
    }

    public boolean hasValue(K key) {
        return at(key).get() != null;
    }

    public boolean tryPut(K key, V value) {
        return at(key).compareAndSet(null, value);
    }

    public boolean tryPut(K key, Supplier<V> valueSupplier) {
        return at(key).compareAndSet(null, valueSupplier.get());
    }

    private AtomicReference<V> at(K key) {
        return map.computeIfAbsent(key, (k) -> new AtomicReference<>(initializer.get()));
    }

}
