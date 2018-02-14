package com.duprasville.limiters.treefill;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class FullTableTest {
    @Test
    void nilTable() {
        assertThat(FullTable.NIL.get(1, 1), is(nullValue()));
        assertThat(FullTable.NIL.get(0L, 0L), is(nullValue()));
        assertThat(FullTable.NIL.tryPut(new Full(1L, 2L, 0L,3L, 42L)), is(false));
    }

}