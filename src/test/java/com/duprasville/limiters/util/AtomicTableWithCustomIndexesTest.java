package com.duprasville.limiters.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AtomicTableWithCustomIndexesTest {
    long[] rounds = new long[]{1L, 2L};
    long[] children = new long[]{36L, 37L, 38L};

    @Test
    void get() {
        AtomicTableWithCustomIndexes<String> table = new AtomicTableWithCustomIndexes<>(rounds, children, "empty");
        assertThat(table.tryPut(1L, 36L, "1,36"), is(true));
        assertThat(table.get(1L, 36L), is(equalTo("1,36")));
        assertThrows(IndexOutOfBoundsException.class, () -> table.get(0, 0));
    }

    @Test
    void getRow() {
        AtomicTableWithCustomIndexes<String> table = new AtomicTableWithCustomIndexes<>(rounds, children, "empty");
        assertThat(table.tryPut(1L, 36L, "1,36"), is(true));
        assertThat(table.getRow(1L), contains("1,36", "empty", "empty"));
        assertThrows(IndexOutOfBoundsException.class, () -> table.getRow(0L));
    }

    @Test
    void tryPut() {
        AtomicTableWithCustomIndexes<String> table = new AtomicTableWithCustomIndexes<>(rounds, children, "empty");
        assertThat(table.tryPut(1L, 36L, "1,36"), is(true));
        assertThat(table.tryPut(1L, 36L, "1,36"), is(false));
        assertThat(table.get(1L, 36L), is(equalTo("1,36")));
        assertThrows(IndexOutOfBoundsException.class, () -> table.tryPut(0, 0, "nope"));
    }

    @Test
    void rowMap() {
        AtomicTableWithCustomIndexes<String> table = new AtomicTableWithCustomIndexes<>(rounds, children, "empty");
        assertThat(table.tryPut(1L, 36L, "1,36"), is(true));
        Map<Long, String> rowMap = table.getRowMap(1L);
        long[] rowKeys = rowMap.keySet().stream().sorted().mapToLong(v -> v).toArray();
        assertArrayEquals(rowKeys, children);
        List<String> values = rowMap.entrySet().stream().sorted(comparingByKey()).map(Map.Entry::getValue).collect(toList());
        assertThat(values, contains("1,36", "empty", "empty"));
    }
}