package com.duprasville.limiters.treefill;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FullTable extends MessageTable<Full>{
    protected FullTable(long[] rounds, long[] children, BiConsumer<Long, List<Full>> consumer) {
        super(rounds, children, null, consumer);
    }

    private FullTable(FullTable original, BiConsumer<Long, List<Full>> consumer) {
        super(original, consumer);
    }

    public FullTable copy(BiConsumer<Long, List<Full>> consumer) {
        return (NIL == this) ? NIL : new FullTable(this, consumer);
    }

    public FullTable(long[] rounds, long[] children) {
        this(rounds, children, (a, b) -> {});
    }

    public boolean tryPut(Full full) {
        return tryPut(full.round, full.src, full);
    }

    private Random randomizer = new Random();
    public boolean tryWithFirstEmpty(long maxRoundInclusive, Consumer<Long> consumer) {
        for (long round = 1L; round <= maxRoundInclusive; round++) {
            List<Long> empties = getEmptyEntries(round);
            if (empties.size() > 0) {
                consumer.accept(empties.get(randomizer.nextInt(empties.size())));
                return true;
            }
        }
        return false;
    }

    public static FullTable NIL = new FullTable(new long[]{}, new long[]{}, (a, b) -> {}) {
        @Override
        public boolean tryPut(Full full) {
            return false;
        }

        @Override
        public boolean tryWithFirstEmpty(long maxRoundInclusive, Consumer<Long> consumer) {
            return false;
        }

        @Override
        public Full get(long row, long col) {
            return null;
        }

        @Override
        public List<Full> getRow(long row) {
            return Collections.emptyList();
        }

        @Override
        public Map<Long, Full> getRowMap(long row) {
            return Collections.emptyMap();
        }

        @Override
        public List<Long> getEmptyEntries(long row) {
            return Collections.emptyList();
        }
    };
}
