package com.duprasville.limiters.util;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.LongStream;

import static com.duprasville.limiters.util.Utils.spread;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

//[seed -1456281816] 14182756 requests per 0 ticks over 0 ticks

// [seed -352463801] 21913167 requests per 201 ticks over 18090 ticks.  Expected: is <1972185031L> but: was <1972185030L>
// java.lang.AssertionError: [seed 838099547] 9 requests per 1 ticks over 2 ticks

class UtilsTest {
    @Test
    void spreads() {
        int seed = new Random().nextInt();
//        int seed = 1772430630;
        Random rand = new Random(seed);
        for (int i = 0; i < 1_000; i++) {
            long requests = rand.nextInt(20) + 1L;
            long perTicks = rand.nextInt(5) + 1L;
            long periods = rand.nextInt(5) + 1L;
            long duration = perTicks * periods;
            long expected = requests * periods;

            long actual = LongStream.range(0, duration).boxed()
                    .reduce(0L, (sum, tick) -> sum + spread(tick, requests, perTicks));

            assertThat(
                    format("[seed %d] %d requests per %d ticks over %d ticks", seed, requests, perTicks, duration),
                    actual,
                    is(expected));
//                    is(both(greaterThanOrEqualTo(expected)).and(lessThanOrEqualTo(expected + 1)))
//            );
        }
    }

    @Test
    void xxx() {
        long requests = 9L; // 250K per 10 sec for 100 sec ~2_500_000
        long perTicks = 1L;
        long periods = 2;
        long duration = perTicks * periods;
        long expected = requests * periods;

        long actual = LongStream.range(0, duration).boxed()
                .reduce(0L, (sum, tick) -> sum + spread(tick, requests, perTicks));

        assertThat(
                format("[seed %d] %d requests per %d ticks over %d ticks", 0, requests, perTicks, duration),
                actual,
                is(expected)
        );
    }

    @Test
    void spreadRpsOverClusterNodes() {
        long rps = 13L;
//        long perServerNodes = (long)(Math.pow(5, (4 + 1)) - 1) / (5 - 1);
        long perServerNodes = 3L;

        long actual = LongStream.range(0, perServerNodes).boxed()
                .reduce(0L, (sum, server) -> sum + spread(server, rps, perServerNodes));

        assertThat(
                format("[seed %d] %d rps per %d servers", 0, rps, perServerNodes),
                actual,
                is(rps)
        );
    }
}
// [seed 1772430630] 49048838 - 49048450 = 388
