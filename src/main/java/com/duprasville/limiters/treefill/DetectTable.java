package com.duprasville.limiters.treefill;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class DetectTable extends MessageTable<Detect> {
    private final static long FIRST = 1L;
    private final static long SECOND = 2L;

    public DetectTable(long[] rounds, BiConsumer<Long, List<Detect>> consumer) {
        super(rounds, new long[]{FIRST, SECOND}, null, consumer);
    }

    private DetectTable(DetectTable original, BiConsumer<Long, List<Detect>> consumer) {
        super(original, consumer);
    }

    public DetectTable copy(BiConsumer<Long, List<Detect>> consumer) {
        return (NIL == this) ? NIL : new DetectTable(this, consumer);
    }

    public DetectTable(long[] rounds) {
        this(rounds, (a, b) -> {});
    }

    public boolean tryPut(Detect detect) {
        return tryPut(detect.round, FIRST, detect) || tryPut(detect.round, SECOND, detect);
    }

    public static DetectTable NIL = new DetectTable(new long[]{}, (a, b) -> {}) {
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
