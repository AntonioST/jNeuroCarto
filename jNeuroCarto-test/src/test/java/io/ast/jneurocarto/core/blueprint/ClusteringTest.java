package io.ast.jneurocarto.core.blueprint;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClusteringTest {

    @Test
    public void unionGroup() {
        var expect = new Clustering(new int[]{0, 0, 1, 1, 0, 0, 1, 1, 0, 0});
        var actual = new Clustering(new int[]{0, 0, 1, 1, 0, 0, 2, 2, 0, 0});
        actual.unionClusteringGroup(1, 2);
        Assertions.assertArrayEquals(expect.clustering(), actual.clustering());
    }
}
