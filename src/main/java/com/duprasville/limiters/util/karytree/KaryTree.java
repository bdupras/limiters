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
    private final int ary;
    private final int height;
    private final long perfectCapacity;

    private KaryTree(int ary, int height) {
        this.ary = ary;
        this.height = height;
        this.perfectCapacity = KL_N(ary, height - 1);
    }

    private KaryTree(int ary, long minCapacity) {
        this.ary = ary;
        this.height = Kn_l(ary, minCapacity-1) + 1;
        this.perfectCapacity = KL_N(ary, height - 1);
    }

    public static KaryTree byHeight(int ary, int height) {
        return new KaryTree(ary, height);
    }

    public static KaryTree byMinCapacity(int ary, long minCapacity) {
        return new KaryTree(ary, minCapacity);
    }

    public int getAry() {
        return ary;
    }

    public int getHeight() {
        return height;
    }

    public long getCapacity() {
        return perfectCapacity;
    }

    public Node getNode(long id) {
        return new Node(id);
    }

    public Level getLevel(int level) {
        return new Level(level);
    }

    public Level getBase() {
        return getLevel(getHeight()-1);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + ary + 'x' + height + ')';
    }

    private static long KL_N(int K, int L) {
        return (pow(K, L + 1) - 1) / (K - 1);
    }

    private static int KN_L(int K, long N) {
        return (log_b(K, (N * (K - 1)) + 1) - 1);
    }

    private static long Kl_nMin(int K, int l) {
        return (0 >= l) ? 0 : KL_N(K, l - 1);
    }

    private static long Kl_nMax(int K, int l) {
        return KL_N(K, l) - 1;
    }

    private static int Kn_l(int K, long n) {
        return KN_L(K, n) + 1;
    }

    private static long Kn_p(int K, long n) {
        return ((n - Kl_nMax(K, Kn_l(K, n) - 1) - 1) / K) + (Kl_nMin(K, Kn_l(K, n) - 1));
    }

    private static long Kn_cMax(int K, long n) {
        return ((n - Kl_nMax(K, Kn_l(K, n) - 1)) * K) + Kl_nMax(K, Kn_l(K, n));
    }

    private static long Kn_cMin(int K, long n) {
        return Kn_cMax(K, n) - (K - 1);
    }

    private static long pow(long base, int exp) {
        if (exp == 0)
            return 1;
        if (exp == 1)
            return base;
        if (0 == exp % 2)
            return pow(base * base, exp / 2); // even base=(base^2)^exp/2
        else
            return base * pow(base * base, exp / 2); // odd base=base*(base^2)^exp/2
    }

    private static int log_b(int base, long n) {
        return (int) (Math.log(n) / Math.log(base));
    }

    class Range<T> {
        private final T min;

        private final T max;

        public Range(T min, T max) {
            this.min = min;
            this.max = max;
        }

        public T getMin() {
            return min;
        }

        public T getMax() {
            return max;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[ " + min + " .. " + max + " ]";
        }
    }

    class Level {
        private final int id;

        public Level(int id) {
            this.id = id;
        }

        public Range<Node> getNodes() {
            return new Range<>(new Node(Kl_nMin(ary, id)), new Node(Kl_nMax(ary, id)));
        }

        public int getId() {
            return id;
        }

        public int getHeight() {
            return id + 1;
        }

        public long getWidth() {
            Range<Node> nodes = getNodes();
            return nodes.getMax().getId() - nodes.getMin().getId() + 1;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + id + ')';
        }
    }

    class Node {
        private final long id;

        Node(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public Level getLevel() {
            return new Level(Kn_l(ary, id));
        }

        public Node getParent() {
            return new Node(Kn_p(ary, id));
        }

        public Range<Node> getChildren() {
            return new Range<>(new Node(Kn_cMin(ary, id)), new Node(Kn_cMax(ary, id)));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + id + ')';
        }
    }
}
