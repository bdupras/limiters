package com.duprasville.limiters.treefill;

import com.duprasville.limiters.util.AtomicTableWithCustomIndexes;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class MessageTable<V> extends AtomicTableWithCustomIndexes<V> {
    private final BiConsumer<Long, List<V>> consumer;
    private final AtomicInteger counters[];

    protected MessageTable(long[] rows, long cols[], V emptyValue, BiConsumer<Long, List<V>> consumer) {
        super(rows, cols, emptyValue);
        this.consumer = consumer;
        this.counters = newCounters(getRows());
    }

    protected MessageTable(MessageTable<V> original, BiConsumer<Long, List<V>> consumer) {
        super(original);
        this.consumer = consumer;
        this.counters = newCounters(original.getRows());
    }

    @Override
    public boolean tryPut(long row, long col, V value) {
        boolean ret = super.tryPut(row, col, value);
        if (ret) {
            if (counters[superRow(row)].incrementAndGet() == getColumns()) {
                consumer.accept(row, getRow(row));
            }
        }
        return ret;
    }

    AtomicInteger[] newCounters(int rows) {
        AtomicInteger[] counters = new AtomicInteger[rows];
        for (int i = 0, len = counters.length; i < len; i++)
            counters[i] = new AtomicInteger();
        return counters;
    }
}
