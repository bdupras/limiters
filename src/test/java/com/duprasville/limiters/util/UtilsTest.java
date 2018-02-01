package com.duprasville.limiters.util;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static com.duprasville.limiters.util.Utils.spread;
import static java.lang.String.format;
import static java.util.stream.LongStream.range;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class UtilsTest {
    @Test
    void spreads() {
        int seed = new Random().nextInt();
        Random rand = new Random(seed);
        for (int i = 0; i < 1_000; i++) {
            long requests = rand.nextInt(20) + 1L;
            long perTicks = rand.nextInt(5) + 1L;
            long periods = rand.nextInt(5) + 1L;
            long duration = perTicks * periods;
            long expected = requests * periods;

            long actual = range(0, duration).boxed()
                    .reduce(0L, (sum, tick) -> sum + spread(requests, tick, perTicks));

            assertThat(
                    format("[seed %d] %d requests per %d ticks over %d ticks", seed, requests, perTicks, duration),
                    actual,
                    is(expected));
        }
    }
}
