package com.duprasville.limiters.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.stream.Collectors.toList;

public class AtomicTableWithCustomIndexes<V> extends AtomicTable<V> {
    private final long[] rows;
    private final long[] cols;

    public AtomicTableWithCustomIndexes(long[] rows, long cols[], V emptyValue) {
        super(rows.length, cols.length, emptyValue);
        this.rows = Arrays.copyOf(rows, rows.length);
        Arrays.sort(this.rows);
        this.cols = Arrays.copyOf(cols, cols.length);
        Arrays.sort(this.cols);
    }

    public AtomicTableWithCustomIndexes(AtomicTableWithCustomIndexes<V> original) {
        super(original);
        this.rows = original.rows;
        this.cols = original.cols;
    }

    public V get(long row, long col) {
        return super.get(superRow(row), superCol(col));
    }

    public List<V> getRow(long row) {
        return super.getRow(superRow(row));
    }

    public Map<Long, V> getRowMap(long row) {
        HashMap<Long, V> ret = new HashMap<>(cols.length);
        for (long c : cols) {
            ret.put(c, get(row, c));
        }
        return ret;
    }

    public List<Long> getEmptyEntries(long row) {
        return getRowMap(row)
                .entrySet()
                .stream()
                .filter(e -> e.getValue() == emptyValue)
                .map(Map.Entry::getKey)
                .collect(toList());
    }

    public boolean tryPut(long row, long col, V value) {
        return super.tryPut(superRow(row), superCol(col), value);
    }

    @Override
    public V get(int row, int col) {
        return this.get((long) row, (long) col);
    }

    @Override
    public List<V> getRow(int row) {
        return this.getRow((long) row);
    }

    @Override
    public boolean tryPut(int row, int col, V value) {
        return this.tryPut((long) row, (long) col, value);
    }

    protected int superRow(long row) {
        return getSuperIndex(rows, row);
    }

    protected int superCol(long col) {
        return getSuperIndex(cols, col);
    }

    private int getSuperIndex(long[] idx, long i) {
        int ret = Arrays.binarySearch(idx, i);
        if (ret < 0) 
        		throw new ArrayIndexOutOfBoundsException();
        return ret;
    }
}
