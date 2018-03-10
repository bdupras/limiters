package com.duprasville.limiters.treefill;

import static com.duprasville.limiters.util.Utils.log_b;
import static com.duprasville.limiters.util.Utils.spread;
import static java.lang.Math.*;

/**
 * A collection of helper functions related to doing the basic math of TreeFill. These functions account for allocating
 * permits to rounds and nodes in whole numbers such that the sum of allocations is equal to the requested totals.
 */
public class TreeFillMath {
    /**
     * Determines the number of Treefill rounds (rounds start at 1 to follow the paper).
     * <p>
     * Ref: 3.2. Detecting triggers in the last round
     * "let iₑ be the first round that satisfies w/2ⁱn ≤ 1, then iₑ = ⌈log₂ (W/N)⌉"
     * <p>
     * "...there can be remaining triggers after the iₑth round ends. Thus, we need one more
     * round to detect all of the remaining triggers. Consequently, the last round of
     * TreeFill is ⌈log₂(W/N)⌉ + 1."
     * <p>
     * When W < N, this is equivalent to only the "one more round" as described above. When
     * W << N, then ⌈log₂(W/N)⌉ < 0. To generalize this function, this implementation uses
     * max(0, ⌈log₂(W/N)⌉) to always return at least one round.
     *
     * @param W number of permits
     * @param N number of nodes
     * @return number of Treefill rounds
     */
    public static int rounds(long W, long N) {
        return 1 + max(0, (int) (ceil(log_b(2.0, (double) W / (double) N))));
    }

    /**
     * Determines ŵᵢ[] from the paper - permits to be detected at the whole cluster at the start of
     * each round. The resulting array should follow [W, W/2ⁱ...] for i = 1..rounds(W,N).
     *
     * @param W cluster-wide permits
     * @param N number of nodes in the cluster
     * @return array of longs [W, W/2ⁱ...] for i = 1..rounds(W,N)
     */
    public static long[] clusterPermitsToDetectPerRound(long W, long N) {
        int I = rounds(W, N);
        return permitsPerRound(W, I);
    }

    /**
     * Determines ŵᵢ/2ⁱN from the paper - permits to be detected per round at a given node m.
     *
     * @param W cluster-wide permits
     * @param m specific node
     * @param N number of nodes in the cluster
     * @return array of longs such that element[0] is node m's fair share of N, and the remaining elements count down
     * by approximately powers of K. The tail elements of the array are guaranteed to sum up to the value at element
     * [0].
     */
    public static long[] nodePermitsToDetectPerRound(long W, long m, long N) {
        int I = rounds(W, N);
        long w = spread(W, m, N); // this node's fair share of the cluster's total permits
        return permitsPerRound(w, I);
    }

    private static long[] permitsPerRound(long permits, long round) {
        long[] ret = new long[1 + toIntExact(round)];
        ret[0] = permits;
        long permitsAllocated = 0L;
        // this loop stops one position short because the final round is calculated as a remainder
        for (int i = 1; i < ret.length - 1; i++) {
            ret[i] = (long) ceil((ret[0] - permitsAllocated) / 2.0); // introduces int division error
            permitsAllocated += ret[i];
        }
        ret[ret.length - 1] = ret[0] - permitsAllocated;
        return ret;
    }
}
