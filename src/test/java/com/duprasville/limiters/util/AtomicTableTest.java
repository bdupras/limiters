package com.duprasville.limiters.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AtomicTableTest {

    @Test
    void empty() {
        AtomicTable<String> empty = new AtomicTable<>(2, 2, "empty");
        assertThat(empty.get(0, 0), is(equalTo("empty")));
        assertThat(empty.get(1, 1), is(equalTo("empty")));
        assertThrows(IndexOutOfBoundsException.class, () -> new AtomicTable<>(2, 2, "empty").get(0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> new AtomicTable<>(2, 2, "empty").get(5, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> new AtomicTable<>(0, 0, "wut").get(0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> new AtomicTable<>(0, 2, "wut").get(0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> new AtomicTable<>(2, 0, "wut").get(0, 0));
    }

    @Test
    void copy() {
        AtomicTable<String> empty = new AtomicTable<>(2, 2, "empty");
        AtomicTable<String> copy = new AtomicTable<>(empty);
        assertThat(copy.get(0, 0), is(equalTo(empty.get(0, 0))));
        assertThat(copy.get(1, 1), is(equalTo(empty.get(1, 1))));
        assertThat(copy.get(1, 1), is(equalTo("empty")));
    }

    @Test
    void tryPut() {
        AtomicTable<String> table = new AtomicTable<>(2, 2, "empty");
        assertThat(table.tryPut(0, 0, "notempty"), is(true));
        assertThat(table.tryPut(0, 0, "alsonotempty"), is(false));
        assertThat(table.tryPut(1, 1, "alsonotempty"), is(true));
        assertThat(table.get(0, 0), is(equalTo("notempty")));
        assertThat(table.get(0, 1), is(equalTo("empty")));
        assertThat(table.get(1, 0), is(equalTo("empty")));
        assertThat(table.get(1, 1), is(equalTo("alsonotempty")));
    }



}