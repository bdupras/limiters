package com.duprasville.limiters2.util;

public class Utils {
    /**
     * Calculates a spread of units over a number of positions. When positions is not perfectly
     * divisible by units, some positions receive an extra unit so that the full number
     * of units is divided amongst positions.
     *
     * Each position receives a whole number of units / positions.
     * Any remainder (rem) is "spread" as evenly as possible across positions.
     * To achieve an even spread, rem and positions are divided by their greatest
     * common denominator (GCD).
     *
     * e.g. when rem=4 and positions=12, then GCD=4, rem/GCD=1, positions/CGD=3
     * Then every "remGCD" in "positionsGCD", or every 1 in 3, positions gets a +1.
     *
     * @return units the given pos receives
     */
    public static long spread(long units, long pos, long positions) {
        long whole = units / positions;
        long rem = units % positions;
        if (rem == 0) {
            return whole;
        } else {
            long gcd = gcd(rem, positions);
            long gcdRem = rem / gcd;
            long gcdPositions = positions / gcd;
            return whole + (((pos % gcdPositions) < gcdRem) ? 1L : 0L);
        }
    }

    /**
     * Greatest Common Denominator
     * @param a any long
     * @param b any long
     * @return the greatest common denominator of a nd b
     */
    public static long gcd(long a, long b) {
        return b == 0L ? a : gcd(b, a % b);
    }

    /**
     * Returns the logarithm base <i>b</i> of a {@code double}
     * value.
     *
     * @param   a   a value
     * @return  the value {@code log_b(a)}.
     */
    public static double log_b(double b, double a) {
        return Math.log(a) / Math.log(b);
    }

}
