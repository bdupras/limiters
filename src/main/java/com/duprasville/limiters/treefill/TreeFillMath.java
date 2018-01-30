package com.duprasville.limiters.treefill;

import com.duprasville.limiters.util.karytree.KaryTree;

public class TreeFillMath {
    public static long rounds(KaryTree karyTree, long permits, long clusterSize) {
        // # of rounds = log_K( W / N ), rounded up
        return (long)(Math.ceil(karyTree.log((double)permits / (double)clusterSize)));
    }
}
