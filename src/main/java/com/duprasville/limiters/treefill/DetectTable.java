package com.duprasville.limiters.treefill;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.util.stream.LongStream.range;

public class DetectTable extends MessageTable<Detect> {
    public DetectTable(long[] rounds, long arity, BiConsumer<Long, List<Detect>> consumer) {
        super(rounds, arityToCols(arity), null, consumer);
    }

    private static long[] arityToCols(long arity) {
        return range(0, arity).toArray();
    }

    private DetectTable(DetectTable original, BiConsumer<Long, List<Detect>> consumer) {
        super(original, consumer);
    }

    public DetectTable copy(BiConsumer<Long, List<Detect>> consumer) {
        return (NIL == this) ? NIL : new DetectTable(this, consumer);
    }

    public DetectTable(long[] rounds, long arity) {
        this(rounds, arity, (a, b) -> {});
    }

    public boolean tryPut(Detect detect) {
        return tryPut(detect.round, detect);
    }

    public static DetectTable NIL = new DetectTable(new long[]{}, 0L, (a, b) -> {}) {
        @Override
        public boolean tryPut(Detect detect) {
            return false;
        }

        @Override
        public Detect get(long row, long col) {
            return null;
        }

        @Override
        public List<Detect> getRow(long row) {
            return Collections.emptyList();
        }

        @Override
        public Map<Long, Detect> getRowMap(long row) {
            return Collections.emptyMap();
        }

        @Override
        public List<Long> getEmptyEntries(long row) {
            return Collections.emptyList();
        }
    };
}
