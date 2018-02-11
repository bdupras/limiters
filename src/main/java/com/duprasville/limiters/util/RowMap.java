package com.duprasville.limiters.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class RowMap<R, K, V> {
    private final int maxWidth;
    private final ConcurrentHashMap<R, ConcurrentHashMap<K, V>> table = new ConcurrentHashMap<>();
    private final Object mutex = new Object();
    private final Consumer<Map<K, V>> onRowFull;

    public RowMap(int maxWidth, Consumer<Map<K, V>> onRowFull) {
        this.maxWidth = maxWidth;
        this.onRowFull = onRowFull;
    }

    public V get(R rowKey, K valueKey) {
        return atRow(rowKey, (row) -> row.get(valueKey));
    }

    public boolean hasValue(R rowKey, K valueKey) {
        return get(rowKey, valueKey) != null;
    }

    public Collection<V> valuesAt(R rowKey) {
        ConcurrentHashMap<K, V> row = table.get(rowKey);
        return (null == row) ? ConcurrentHashMap.newKeySet() : row.values();
    }

    public Collection<K> keysAt(R rowKey) {
        ConcurrentHashMap<K, V> row = table.get(rowKey);
        return (null == row) ? ConcurrentHashMap.newKeySet() : Collections.list(row.keys());
    }

    public boolean tryPut(R rowKey, K valueKey, V value) {
        if (maxWidth <= 0) return false;
        ConcurrentHashMap<K, V> row = ensureRow(rowKey);
        boolean callOnRowFull = false;
        if (row.size() <= maxWidth) {
            boolean ret = false;
            synchronized (mutex) { // optimization - could sync per row
                if (row.size() < maxWidth) {
                    V prevValue = row.putIfAbsent(valueKey, value);
                    if ((null == prevValue) && (row.size() == maxWidth)) {
                        callOnRowFull = true;
                    }
                    // Putting the same value multiple times is ok (idempotency)
                    // Putting a different value is not
                    ret = (null == prevValue) || (value == prevValue);
                }
            }
            if (callOnRowFull) {
                onRowFull.accept(row);
            }
            return ret;
        }
        return value == row.get(valueKey);
    }

    private <T> T atRow(R rowKey, Function<ConcurrentHashMap<K, V>, T> function) {
        ConcurrentHashMap<K, V> row = table.get(rowKey);
        return (null == row) ? null : function.apply(row);
    }

    private ConcurrentHashMap<K, V> ensureRow(R rowKey) {
        return table.computeIfAbsent(rowKey, r -> new ConcurrentHashMap<>());
    }
}
