package com.duprasville.limiters2.treefill2;

import static com.duprasville.limiters.util.Utils.log_b;
import static com.duprasville.limiters.util.Utils.spread;
import static java.lang.Math.*;

/**
 * A collection of helper functions related to doing the basic math of TreeFill. These functions account for allocating
 * permits to rounds and nodes in whole numbers such that the sum of allocations is equal to the requested totals.
 */
public class TreeFillMath {
    /**
     * Determines iₑ from the paper - i.e. the last round for rounds = 1..iₑ.
     *
     * @param W number of permits
     * @param N number of nodes
     * @return number of rounds
     */
    public static int rounds(long W, long N) {
        if (W <= N) {
            return 1;
        } else {
            return (int) ceil(log_b(2.0, (double) W / (double) N)) + 1;
        }
    }

    /**
     * Determines ŵᵢ from the paper - permits remaining to be detected at the whole cluster at the start of
     * each round.
     *
     * <p>
     * note: the reason for the element[I] == 0L relates to an edge case, when all these conditions are met:
     * 1. W / N is not a whole number, thus some nodes allocate an extra permit in their "fair share" (heavy nodes)
     * 2. Heavy node fair shares are a power of 2 >= 4L resulting in a final round of 2 permits, e.g.:
     *    light nodes ŵᵢm = [7, 4, 2, 1]
     *    heavy nodes ŵᵢm = [8, 4, 2, 2]
     * 3. Clients request a single permit per tryAcquire()
     * 4. Client requests are not perfectly balanced across the cluster, and the bias favors heavy nodes.
     *
     * <p>
     * In this scenario heavy nodes would send Detects for every 2 permits acquired rather than every 1. The detect tree
     * would not fill even when W permits have been acquired
     *
     * <p>
     * One fix for this scenario is to extend the number of rounds by 1, which causes all such nodes to break down their
     * final rounds to 1 and 1, e.g. :
     *    light nodes ŵᵢm = [7, 4, 2, 1, 0]
     *    heavy nodes ŵᵢm = [8, 4, 2, 1, 1]
     *
     * <p>
     * As long as the Detect and Full messages carry their aggregate number of permits they represent,
     * the root node can detect that W permits have been acquired with or without this final round.
     *
     * @param W cluster-wide permits
     * @param N number of nodes in the cluster
     * @return array of longs such that element[0] is W, elements[1..I-1] successively halve their preceding elements,
     * and element element[I] is always 0L (*see note). The tail elements of the array are guaranteed to sum
     * up to the value at element [0].
     */
    public static long[] clusterPermitsToDetectPerRound(long W, long N) {
        // resulting array == [W, W/2^1, W/2^2, ..., W/2^i, 0L]
        // element [0] is the full cluster limit
        // element [I] is always 0L
        // - covers a corner case
        // - nodes with a fair share at element [i] equal to 2 get elements [..., i, i+1] set to [..., 1L, 1L]

        int I = rounds(W, N);
        if (I < 1) {
            throw new UnsupportedOperationException("Thar be a bug here");
        } else if (1 == I) {
            return new long[]{W, W, 0L};
        }
        long[] w = new long[I + 1];
        // loop stops short, last entry is calculated as a remainder to account for int division error
        long permitsAllocated = 0L;
        int remainderIndex = w.length - 2;
        for (int i = 0; i <= w.length - 3; i++) {
            w[i] = (long) ceil(W / pow(2, i)); // introduces int division error
            if (i > 0) permitsAllocated += w[i];
            if (w[i] == 0) remainderIndex = i;
        }
        w[remainderIndex] = w[0] - permitsAllocated; // adjust for int division error by allocating remainder
        return w;
    }

    /**
     * Determines ŵᵢ/2N from the paper - permits remaining to be detected per round at a given node m.
     *
     * @param W cluster-wide permits
     * @param m specific node
     * @param N number of nodes in the cluster
     * @return array of longs such that element[0] is node m's fair share of N, and the remaining elements count down
     * by approximately half. The tail elements of the array are guaranteed to sum up to the value at element
     * [0].
     */
    public static long[] nodePermitsToDetectPerRound(long W, long m, long N) {
        long[] w = clusterPermitsToDetectPerRound(W, N);
        long[] wm = new long[w.length];
        wm[0] = spread(w[0], m, N); // [0] element is this node's fair share of W
        long permitsAllocated = 0L;
        int remainderIndex = wm.length - 1;
        for (int i = 1; i < wm.length - 1; i++) { // [1]..[I-2] elements are each half of the previously unallocated
            wm[i] = (long) ceil((wm[0] - permitsAllocated) / 2.0);
            permitsAllocated += wm[i];
            if (wm[i] == 0) remainderIndex = i;
        }
        wm[remainderIndex] = max(0L, wm[0] - permitsAllocated); // last element [I] is the remaining unallocated
        return wm;
    }
}
