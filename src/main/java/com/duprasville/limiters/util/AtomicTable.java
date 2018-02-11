package com.duprasville.limiters.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;

public class AtomicTable<V> {
    protected final AtomicReference<V>[][] table;
    protected final V emptyValue;

    public AtomicTable(int rows, int cols, V emptyValue) {
        this.emptyValue = emptyValue;
        this.table = forEachWithIndex(newTable(rows, cols), (table, row, col, valRef) ->
                table[row][col] = new AtomicReference<>(emptyValue));
    }

    public AtomicTable(AtomicTable<V> original) {
        this.emptyValue = original.emptyValue;
        this.table = forEachWithIndex(
                newTable(original.getRows(), original.getRows() > 0 ? original.table[0].length : 0),
                (table, row, col, valRef) ->
                        table[row][col] = new AtomicReference<>(original.table[row][col].get())
        );
    }

    public V get(int row, int col) {
        return at(row, col).get();
    }

    public int getRows() {
        return table.length;
    }

    public int getColumns() {
        return getRows() > 0 ? table[0].length : 0;
    }

    @SuppressWarnings("unchecked")
    public List<V> getRow(int row) {
        return Arrays.stream(table[row]).map(AtomicReference::get).collect(toList());
    }

    public boolean tryPut(int row, int col, V value) {
        return at(row, col).compareAndSet(emptyValue, value);
    }

    private AtomicReference<V> at(int row, int col) {
        return table[row][col];
    }

    @SuppressWarnings("unchecked")
    private static <V> AtomicReference<V>[][] newTable(int rows, int cols) {
        return new AtomicReference[rows][cols];
    }

    private static <E> AtomicReference<E>[][] forEachWithIndex(AtomicReference<E>[][] table, ForEachIndex<AtomicReference<E>> consumer) {
        int rows = table.length;
        int cols = rows > 0 ? table[0].length : 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                consumer.accept(table, row, col, table[row][col]);
            }
        }
        return table;
    }

    interface ForEachIndex<E> {
        void accept(E[][] table, int row, int col, E value);
    }

}

