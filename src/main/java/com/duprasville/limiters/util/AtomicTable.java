package com.duprasville.limiters.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;

public class AtomicTable<R, C, V> {
    private final ConcurrentHashMap<R, ConcurrentHashMap<C, AtomicReference<V>>> table = new ConcurrentHashMap<>();

    public V get(R rowKey, C colKey) {
        return at(rowKey, colKey).get();
    }

    public boolean tryPut(R rowKey, C colKey, V value) {
        return at(rowKey, colKey).compareAndSet(null, value);
    }

    public boolean hasValue(R rowKey, C colKey) {
        return at(rowKey, colKey).get() != null;
    }

    private ConcurrentHashMap<C, AtomicReference<V>> getRow(R rowKey) {
        return table.computeIfAbsent(rowKey, (r) -> new ConcurrentHashMap<>());
    }

    private AtomicReference<V> at(R rowKey, C colKey) {
        return getRow(rowKey).computeIfAbsent(colKey, (c) -> new AtomicReference<>());
    }

    public List<V> valuesAt(R rowKey) {
        return getRow(rowKey)
                .values()
                .stream()
                .filter(ref -> ref.get() != null)
                .map(AtomicReference::get)
                .collect(toList());
    }
}
