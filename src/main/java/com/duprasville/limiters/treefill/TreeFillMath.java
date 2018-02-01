package com.duprasville.limiters.treefill;

import static com.duprasville.limiters.util.Utils.log_b;
import static com.duprasville.limiters.util.Utils.spread;
import static java.lang.Math.*;

/**
 * A collection of helper functions related to doing the basic math of TreeFill. These functions account for allocating
 * triggers to rounds and nodes in whole numbers such that the sum of allocations is equal to the requested totals.
 */
public class TreeFillMath {
    /**
     * Determines iₑ from the paper - i.e. the last round for rounds = 1..iₑ.
     * @param W number of triggers
     * @param N number of nodes
     * @return number of rounds
     */
    public static long rounds(long W, long N) {
        if (W <= N) {
            return 1;
        } else {
            return (long) ceil(log_b(2.0, (double) W / (double) N)) + 1L;
        }
    }

    /**
     * Determines ŵᵢ i.e. w[i] from the paper - triggers remaining to be detected per round at the whole cluster.
     * @param W cluster-wide triggers
     * @param N number of nodes in the cluster
     * @return array of longs such that element[0] is W, and the remaining elements count down by approximately half.
     * The tail elements of the array are guaranteed to sum up to the value at element [0].
     */
    public static long[] clusterRemainingTriggersPerRound(long W, long N) {
        int I = toIntExact(rounds(W, N));
        if (I < 1) {
            throw new UnsupportedOperationException("Thar be a bug here");
        } else if (1 == I) {
            return new long[]{W};
        }
        long[] w = new long[I];
        // loop stops short, last entry is calculated as a remainder account for int division error
        long permitsAllocated = 0L;
        int remainderIndex = w.length - 1;
        for (int i = 0; i <= w.length - 2; i++) {
            w[i] = (long) ceil(W / pow(2, i)); // introduces int division error
            if (i > 0) permitsAllocated += w[i];
            if (w[i] == 0) remainderIndex = i;
        }
        w[remainderIndex] = w[0] - permitsAllocated; // adjust for int division error by allocating remainder
        return w;
    }

    /**
     * Determines ŵᵢ/2N i.e. nw[i] from the paper - triggers remaining to be detected per round at a given node m.
     *
     * @param W cluster-wide triggers
     * @param m specific node
     * @param N number of nodes in the cluster
     * @return array of longs such that element[0] is node m's fair share of N, and the remaining elements count down
     * by approximately half. The tail elements of the array are guaranteed to sum up to the value at element
     * [0].
     */
    public static long[] nodeRemainingTriggersPerRound(long W, long m, long N) {
        long[] w = clusterRemainingTriggersPerRound(W, N);
        long[] nw = new long[w.length];
        nw[0] = spread(w[0], m, N); // [0] element is this node's fair share of W
        long permitsAllocated = 0L;
        int remainderIndex = nw.length - 1;
        for (int i = 1; i < nw.length - 1; i++) { // [1]..[I-2] elements are each half of the previously unallocated
            nw[i] = (long) ceil((nw[0] - permitsAllocated) / 2.0);
            permitsAllocated += nw[i];
            if (nw[i] == 0) remainderIndex = i;
        }
        nw[remainderIndex] = max(0L, nw[0] - permitsAllocated); // last [I-1] element is the remaining unallocated
        return nw;
    }
}
