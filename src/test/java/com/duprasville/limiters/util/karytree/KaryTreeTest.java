package com.duprasville.limiters.util.karytree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.LongStream;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

class KaryTreeTest {

    void things() {
        KaryTree tree = KaryTree.byHeight(5L, 5L);
        long levelId = tree.levelOfNode(5L);
        long nodeId = 6L;
        System.out.println("tree: " + tree);
        System.out.println("ary: " + tree.getAry());
        System.out.println("height: " + tree.getHeight());
        System.out.println("capacity: " + tree.getCapacity());
        System.out.println("");
        System.out.println("level: " + levelId);
        System.out.println("nodes: " + Arrays.toString(tree.nodesOfLevel(levelId)));
        System.out.println("");
        System.out.println("node: " + nodeId);
        System.out.println("level: " + tree.levelOfNode(nodeId));
        System.out.println("parent: " + tree.parentOfNode(nodeId));
        System.out.println("children: " + Arrays.toString(tree.childrenOfNode(nodeId)));
        System.out.println("cousins: " + Arrays.toString(tree.nodesOfLevel(tree.levelOfNode(nodeId))));

        KaryLayout layout = new KaryLayout(tree);
        System.out.println(format("layout relX: %.4f", layout.relX(nodeId)));
        System.out.println(format("layout relY: %.4f", layout.relY(nodeId)));
    }

    @Test
    void singleNodeTree() {
        LongStream.rangeClosed(2L, 12L).boxed().forEachOrdered(ary -> {
            KaryTree tree = KaryTree.byHeight(ary, 1L);
            assertThat(tree.getCapacity(), equalTo(1L));
            assertThat(tree.getAry(), equalTo(ary));
            assertThat(tree.getHeight(), equalTo(1L));
            assertThat(tree.getNodesOfBaseLevel(), equalTo(0L));
            assertThat(tree.getWidth(), equalTo(1L));
        });
    }

    @Test
    void tree5x2() {
        KaryTree tree = KaryTree.byHeight(5L, 2L);
        assertThat(tree.getCapacity(), equalTo(6L));
        assertThat(tree.getAry(), equalTo(5L));
        assertThat(tree.getHeight(), equalTo(2L));
        assertThat(tree.getNodesOfBaseLevel(), equalTo(1L));
        assertThat(tree.getWidth(), equalTo(5L));
        assertThat(tree.parentOfNode(0L), equalTo(0L));
        assertThat(tree.parentOfNode(1L), equalTo(0L));
        assertThat(tree.parentOfNode(5L), equalTo(0L));
        assertThat(tree.nodesOfLevel(0L), equalTo(new long[]{0L}));
        assertThat(tree.childrenOfNode(0L), equalTo(new long[]{1L, 2L, 3L, 4L, 5L}));
        assertThat(tree.nodesOfLevel(tree.getNodesOfBaseLevel()), equalTo(new long[]{1L, 2L, 3L, 4L, 5L}));
    }

    @Test
    void tree5x3() {
        KaryTree tree = KaryTree.byHeight(5L, 3L);
        assertThat(tree.getCapacity(), equalTo(31L));
        assertThat(tree.getAry(), equalTo(5L));
        assertThat(tree.getHeight(), equalTo(3L));
        assertThat(tree.getNodesOfBaseLevel(), equalTo(2L));
        assertThat(tree.getWidth(), equalTo(25L));
        assertThat(tree.nodesOfLevel(0L), equalTo(new long[]{0L}));
        assertThat(tree.childrenOfNode(0L), equalTo(new long[]{1L, 2L, 3L, 4L, 5L}));
        long[] baseNodes = tree.nodesOfLevel(tree.getNodesOfBaseLevel());
        assertThat(baseNodes.length, equalTo(25));
        assertThat(baseNodes[0], equalTo(6L));
        assertThat(baseNodes[baseNodes.length - 1], equalTo(30L));
    }


    @Test
    void tree2x2() {
        KaryTree tree = KaryTree.byHeight(2L, 2L);
        assertThat(tree.getCapacity(), equalTo(3L));
        assertThat(tree.getAry(), equalTo(2L));
        assertThat(tree.getHeight(), equalTo(2L));
        assertThat(tree.getNodesOfBaseLevel(), equalTo(1L));
        assertThat(tree.getWidth(), equalTo(2L));
        long[] baseNodes = tree.nodesOfLevel(tree.getNodesOfBaseLevel());
        assertThat(baseNodes.length, equalTo(2));
        assertThat(baseNodes[0], equalTo(1L));
        assertThat(baseNodes[baseNodes.length - 1], equalTo(2L));
    }

    @Test
    void tree2x3() {
        KaryTree tree = KaryTree.byHeight(2L, 3L);
        assertThat(tree.getCapacity(), equalTo(7L));
        assertThat(tree.getAry(), equalTo(2L));
        assertThat(tree.getHeight(), equalTo(3L));
        assertThat(tree.getNodesOfBaseLevel(), equalTo(2L));
        assertThat(tree.getWidth(), equalTo(4L));
        long[] baseNodes = tree.nodesOfLevel(tree.getNodesOfBaseLevel());
        assertThat(baseNodes.length, equalTo(4));
        assertThat(baseNodes[0], equalTo(3L));
        assertThat(baseNodes[baseNodes.length - 1], equalTo(6L));
    }

    @Test
    void tree2x4() {
        KaryTree tree = KaryTree.byHeight(2L, 4L);
        assertThat(tree.getCapacity(), equalTo(15L));
        assertThat(tree.getAry(), equalTo(2L));
        assertThat(tree.getHeight(), equalTo(4L));
        assertThat(tree.getNodesOfBaseLevel(), equalTo(3L));
        assertThat(tree.getWidth(), equalTo(8L));
        long[] baseNodes = tree.nodesOfLevel(tree.getNodesOfBaseLevel());
        assertThat(baseNodes.length, equalTo(8));
        assertThat(baseNodes[0], equalTo(7L));
        assertThat(baseNodes[baseNodes.length - 1], equalTo(14L));
    }

    @Test
    void treeByCapacity() {
        KaryTree tree = KaryTree.byMinCapacity(5L, 25L);
        assertThat(tree.getCapacity(), equalTo(31L));
        assertThat(tree.getAry(), equalTo(5L));
        assertThat(tree.getHeight(), equalTo(3L));
        assertThat(tree.getNodesOfBaseLevel(), equalTo(2L));
        assertThat(tree.getWidth(), equalTo(25L));
        long[] baseNodes = tree.nodesOfLevel(tree.getNodesOfBaseLevel());
        assertThat(baseNodes.length, equalTo(25));
        assertThat(baseNodes[0], equalTo(6L));
        assertThat(baseNodes[baseNodes.length - 1], equalTo(30L));
    }

    @Test
    void byHeightByCapacity5H5() {
        KaryTree tree = KaryTree.byHeight(5L, 5L);
        assertThat(tree.getCapacity(), equalTo(781L));
        assertThat(tree.getHeight(), equalTo(5L));
        long[] baseNodes = tree.nodesOfLevel(tree.getNodesOfBaseLevel());
        assertThat(baseNodes.length, equalTo(625));
        assertThat(baseNodes[0], equalTo(156L));
        assertThat(baseNodes[baseNodes.length - 1], equalTo(780L));
    }

    @Test
    void byByCapacity5C781() {
        KaryTree tree = KaryTree.byMinCapacity(5L, 781L);
        assertThat(tree.getCapacity(), equalTo(781L));
        assertThat(tree.getHeight(), equalTo(5L));
        long[] baseNodes = tree.nodesOfLevel(tree.getNodesOfBaseLevel());
        assertThat(baseNodes.length, equalTo(625));
        assertThat(baseNodes[0], equalTo(156L));
        assertThat(baseNodes[baseNodes.length - 1], equalTo(780L));
    }
}
