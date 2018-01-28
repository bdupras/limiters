package com.duprasville.limiters.util.karytree;

/*
 * Models a K-ary tree of H height with a perfect-tree capacity of N nodes, where
 *   - each node is identified by n = 0..N-1
 *   - the root of the tree is a single node n = 0
 *   - nodes have K children c[0..K-1]
 *   - nodes are numbered breadth-first
 *   - each non-root node has one parent p
 *   - the tree has l levels 0..H-1 inclusive
 *
 * l=0                                       n=00        K=3, H=4, N=40
 *                                          ┌──┼───┐
 * l=1                                      01 02 03
 *                                    ┌─────┘  │   └─────┐
 *                                 ┌──┼──┐  ┌──┼───┐ ┌───┼──┐
 * l=2                             04 05 06 07 08 09 10 11 12
 *         ┌───────────────────────┘  │  │  │  │   │ │   │  └───────────────────────┐
 *         │        ┌─────────────────┘  │  │  │   │ │   └─────────────────┐        │
 *         │        │        ┌───────────┘  │  │   │ └────────────┐        │        │
 *         │        │        │        ┌─────┘  │   └─────┐        │        │        │
 *      ┌──┼───┐ ┌──┼───┐ ┌──┼───┐ ┌──┼───┐ ┌──┼───┐ ┌───┼──┐ ┌───┼──┐ ┌───┼──┐ ┌───┼──┐
 * l=3  13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39
 */

public class KaryTree {
    private final long ary;
    private final long height;
    private final long perfectCapacity;

    private KaryTree(long ary, long height) {
        this.ary = ary;
        this.height = height;
        this.perfectCapacity = KL_N(ary, height - 1);
    }

    public static KaryTree byHeight(long ary, long height) {
        return new KaryTree(ary, height);
    }

    public static KaryTree byMinCapacity(long ary, long minCapacity) {
        long height = Kn_l(ary, minCapacity - 1L) + 1L;
        return new KaryTree(ary, height);
    }

    public long getAry() {
        return ary;
    }

    public long getHeight() {
        return height;
    }

    public long getWidth() {
        return widthOfLevel(getBaseLevel());
    }

    public long getCapacity() {
        return perfectCapacity;
    }

    public long parentOfNode(long nodeId) {
        return Kn_p(ary, nodeId);
    }

    public long levelOfNode(long nodeId) {
        return Kn_l(ary, nodeId);
    }

    public long[] childrenOfNode(long nodeId) {
        long min = Kn_cMin(ary, nodeId);
        long max = Kn_cMax(ary, nodeId);
        return minMaxArray(min, max);
    }

    public long levelIndexOfNode(long nodeId) {
        long levelId = levelOfNode(nodeId);
        long minNodeIdOfLevel = Kl_nMin(ary, levelId);
        return nodeId - minNodeIdOfLevel;
    }

    public long widthOfLevel(long levelId) {
        long min = Kl_nMin(ary, levelId);
        long max = Kl_nMax(ary, levelId);
        return max - min + 1;
    }

    public long[] nodesOfLevel(long levelId) {
        long min = Kl_nMin(ary, levelId);
        long max = Kl_nMax(ary, levelId);
        return minMaxArray(min, max);
    }

    public long getBaseLevel() {
        return getHeight() - 1L;
    }

    private long[] minMaxArray(long min, long max) {
        long width = max - min + 1;
        if (width > Integer.MAX_VALUE) throw new ArrayIndexOutOfBoundsException("level is too long");
        int w = (int) width;
        long[] arr = new long[w];
        for (int i = 0; i < w; i++) {
            arr[i] = min + i;
        }
        return arr;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + ary + 'x' + height + ')';
    }

    private static long KL_N(long K, long L) {
        return (pow(K, L + 1L) - 1L) / (K - 1);
    }

    private static long KN_L(long K, long N) {
        return (log_b(K, (N * (K - 1)) + 1) - 1);
    }

    private static long Kl_nMin(long K, long l) {
        return (0 >= l) ? 0 : KL_N(K, l - 1L);
    }

    private static long Kl_nMax(long K, long l) {
        return KL_N(K, l) - 1;
    }

    private static long Kn_l(long K, long n) {
        return KN_L(K, n) + 1;
    }

    private static long Kn_p(long K, long n) {
        return ((n - Kl_nMax(K, Kn_l(K, n) - 1) - 1) / K) + (Kl_nMin(K, Kn_l(K, n) - 1));
    }

    private static long Kn_cMax(long K, long n) {
        return ((n - Kl_nMax(K, Kn_l(K, n) - 1)) * K) + Kl_nMax(K, Kn_l(K, n));
    }

    private static long Kn_cMin(long K, long n) {
        return Kn_cMax(K, n) - (K - 1);
    }

    private static long pow(long base, long exp) {
        if (exp == 0)
            return 1;
        if (exp == 1)
            return base;
        if (0 == exp % 2)
            return pow(base * base, exp / 2); // even base=(base^2)^exp/2
        else
            return base * pow(base * base, exp / 2); // odd base=base*(base^2)^exp/2
    }

    private static long log_b(long base, long n) {
        return (long) (Math.log(n) / Math.log(base));
    }

}
