package com.duprasville.limiters.util;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

class AtomicIncrementorWithThresholdsTest {

    @Test
    void addAndGetByOne() {
        List<Long> expected = Arrays.asList(1L, 2L, 4L);
        List<Long> actual = new ArrayList<>(expected.size());
        AtomicIncrementorWithThresholds it = new AtomicIncrementorWithThresholds(
                0L,
                expected,
                actual::add
                );

        for (int i = 0; i < 5; i++) {
            it.addAndGet(1L);
        }

        assertThat(actual.toArray(), is(equalTo(expected.toArray())));
    }

    @Test
    void addAndGetByTwo() {
        List<Long> expected = Arrays.asList(1L, 2L, 4L);
        List<Long> actual = new ArrayList<>(expected.size());
        AtomicIncrementorWithThresholds it = new AtomicIncrementorWithThresholds(
                0L,
                expected,
                actual::add
                );

        for (int i = 0; i < 5; i++) {
            it.addAndGet(2L);
        }

        assertThat(actual.toArray(), is(equalTo(new Long[]{2L, 4L})));
    }

    @Test
    void addAndGetByMany() {
        List<Long> expected = Arrays.asList(1L, 2L, 4L);
        List<Long> actual = new ArrayList<>(expected.size());
        AtomicIncrementorWithThresholds it = new AtomicIncrementorWithThresholds(
                0L,
                expected,
                actual::add
                );
        it.addAndGet(1000L);
        assertThat(actual.toArray(), is(equalTo(new Long[]{4L})));
    }
}