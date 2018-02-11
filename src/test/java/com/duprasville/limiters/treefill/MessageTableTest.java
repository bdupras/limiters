package com.duprasville.limiters.treefill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

class MessageTableTest {
    @Test
    void callBack() {
        AtomicLong lastFullRow = new AtomicLong(-1);
        AtomicReference<List<String>> lastFullValues = new AtomicReference<>();
        BiConsumer<Long, List<String>> consumer = (row, vals) -> {
            lastFullRow.set(row);
            lastFullValues.set(vals);
        };

        long[] rounds = new long[]{1L, 2L};
        long[] children = new long[]{35L, 36L, 37L};

        MessageTable<String> table = new MessageTable<>(rounds, children, "empty", consumer);
        assertThat(lastFullRow.get(), is(equalTo(-1L)));
        assertThat(lastFullValues.get(), is(equalTo(null)));

        assertThat(table.tryPut(1L, 35L, "1,35"), is(true));
        assertThat(lastFullRow.get(), is(equalTo(-1L)));
        assertThat(table.tryPut(1L, 36L, "1,36"), is(true));
        assertThat(lastFullRow.get(), is(equalTo(-1L)));
        assertThat(table.tryPut(1L, 37L, "1,37"), is(true));
        assertThat(lastFullRow.get(), is(equalTo(1L)));
        assertThat(lastFullValues.get(), contains("1,35", "1,36", "1,37"));

        assertThat(table.tryPut(2L, 35L, "2,35"), is(true));
        assertThat(lastFullRow.get(), is(equalTo(1L)));
        assertThat(table.tryPut(2L, 36L, "2,36"), is(true));
        assertThat(lastFullRow.get(), is(equalTo(1L)));
        assertThat(table.tryPut(2L, 37L, "2,37"), is(true));
        assertThat(lastFullRow.get(), is(equalTo(2L)));
        assertThat(lastFullValues.get(), contains("2,35", "2,36", "2,37"));
    }

    @Test
    void copy() {
        AtomicLong lastFullRow = new AtomicLong(-1);
        AtomicReference<List<String>> lastFullValues = new AtomicReference<>();
        BiConsumer<Long, List<String>> consumer = (row, vals) -> {
            lastFullRow.set(row);
            lastFullValues.set(vals);
        };

        long[] rounds = new long[]{1L, 2L};
        long[] children = new long[]{35L, 36L, 37L};

        MessageTable<String> original = new MessageTable<>(rounds, children, "empty", (a, b) -> {});
        MessageTable<String> copy = new MessageTable<>(original, consumer);

        assertThat(lastFullRow.get(), is(equalTo(-1L)));
        assertThat(lastFullValues.get(), is(equalTo(null)));

        assertThat(original.tryPut(1L, 35L, "1,35"), is(true));
        assertThat(original.tryPut(1L, 36L, "1,36"), is(true));
        assertThat(original.tryPut(1L, 37L, "1,37"), is(true));

        assertThat(lastFullRow.get(), is(equalTo(-1L)));
        assertThat(lastFullValues.get(), is(equalTo(null)));

        assertThat(copy.tryPut(2L, 35L, "2,35"), is(true));
        assertThat(copy.tryPut(2L, 36L, "2,36"), is(true));
        assertThat(copy.tryPut(2L, 37L, "2,37"), is(true));

        assertThat(lastFullRow.get(), is(equalTo(2L)));
        assertThat(lastFullValues.get(), contains("2,35", "2,36", "2,37"));
    }
}