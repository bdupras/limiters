package com.duprasville.limiters.util.karytree;

import com.duprasville.limiters.vizualization.KaryNodeAppearance;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class KaryNodeAppearanceTest {
    @Test
    void treeLayout() {
        KaryTree tree = KaryTree.byHeight(2, 4);
        KaryNodeAppearance layout = new KaryNodeAppearance(tree);

        // Level 0
        assertThat(layout.relX(0), equalTo(1.0 / 2));
        assertThat(layout.relY(0), equalTo(0.0));

        // Level 1
        assertThat(layout.relX(1), equalTo(1.0 / 3));
        assertThat(layout.relX(2), equalTo(2.0 / 3));
        assertThat(layout.relY(1), equalTo(1.0 / 3));
        assertThat(layout.relY(2), equalTo(1.0 / 3));

        //Level 2
        assertThat(layout.relX(3), equalTo(1.0 / 5));
        assertThat(layout.relX(4), equalTo(2.0 / 5));
        assertThat(layout.relX(5), equalTo(3.0 / 5));
        assertThat(layout.relX(6), equalTo(4.0 / 5));
        assertThat(layout.relY(3), equalTo(2.0 / 3));
        assertThat(layout.relY(4), equalTo(2.0 / 3));
        assertThat(layout.relY(5), equalTo(2.0 / 3));
        assertThat(layout.relY(6), equalTo(2.0 / 3));

        //Level 3
        assertThat(layout.relX(7), equalTo(1.0 / 9));
        assertThat(layout.relX(8), equalTo(2.0 / 9));
        assertThat(layout.relX(9), equalTo(3.0 / 9));
        assertThat(layout.relX(10), equalTo(4.0 / 9));
        assertThat(layout.relX(11), equalTo(5.0 / 9));
        assertThat(layout.relX(12), equalTo(6.0 / 9));
        assertThat(layout.relX(13), equalTo(7.0 / 9));
        assertThat(layout.relX(14), equalTo(8.0 / 9));
        assertThat(layout.relY(7), equalTo(3.0 / 3));
        assertThat(layout.relY(8), equalTo(3.0 / 3));
        assertThat(layout.relY(9), equalTo(3.0 / 3));
        assertThat(layout.relY(10), equalTo(3.0 / 3));
        assertThat(layout.relY(11), equalTo(3.0 / 3));
        assertThat(layout.relY(12), equalTo(3.0 / 3));
        assertThat(layout.relY(13), equalTo(3.0 / 3));
        assertThat(layout.relY(14), equalTo(3.0 / 3));
    }
}