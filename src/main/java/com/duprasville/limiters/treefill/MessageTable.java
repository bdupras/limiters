package com.duprasville.limiters.treefill;

import com.duprasville.limiters.util.AtomicTableWithCustomIndexes;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.toList;

public class MessageTable<V> extends AtomicTableWithCustomIndexes<V> {
    private final BiConsumer<Long, List<V>> consumer;

    protected MessageTable(long[] rows, long cols[], V emptyValue, BiConsumer<Long, List<V>> consumer) {
        super(rows, cols, emptyValue);
        this.consumer = consumer;
    }

    protected MessageTable(MessageTable<V> original, BiConsumer<Long, List<V>> consumer) {
        super(original);
        this.consumer = consumer;
    }

    @Override
    public boolean tryPut(long row, long col, V value) {
        boolean ret = super.tryPut(row, col, value);
        if (ret) {
            if (getEmptyEntries(row).isEmpty()) {
                consumer.accept(row, getRow(row));
            }
        }
        return ret;
    }

    public boolean isRowFull(long row) {
        List<V> rows = getRow(row);
        List<V> empties = rows.stream().filter(Objects::isNull).collect(toList());
        return empties.size() == 0;
    }
}
