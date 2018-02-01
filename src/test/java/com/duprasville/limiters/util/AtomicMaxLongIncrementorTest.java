package com.duprasville.limiters.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

class AtomicMaxLongIncrementorTest {
    @Test
    void tryIncrement() {
        long MAX = 100;
        AtomicMaxLongIncrementor x = new AtomicMaxLongIncrementor(0, MAX);
        assertThat(x.get(), is (0L));
        assertThat(x.tryIncrement(1L), is(1L));
        assertThat(x.tryIncrement(MAX), is(-1L));
        assertThat(x.get(), is (1L));
    }
}