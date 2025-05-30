package io.ast.jneurocarto.core.blueprint;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static io.ast.jneurocarto.core.blueprint.BlueprintAssertion.assertClusteringEquals;
import static io.ast.jneurocarto.core.blueprint.BlueprintAssertion.fromShape;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BlueprintAssertionTest {


    @Test
    public void testInternalClusteringAssertor() {
        var bp = fromShape(1, 5, 2);
        assertClusteringEquals(bp, new int[]{
          1, 1,
          2, 2,
          0, 0,
          4, 4,
          5, 5,
        }, new int[]{
          1, 1,
          2, 2,
          0, 0,
          4, 4,
          5, 5,
        });
        assertClusteringEquals(bp, new int[]{
          5, 5,
          4, 4,
          0, 0,
          2, 2,
          1, 1,
        }, new int[]{
          1, 1,
          2, 2,
          0, 0,
          4, 4,
          5, 5,
        });

        assertThrows(AssertionFailedError.class, () -> {
            assertClusteringEquals(bp, new int[]{
              5, 5,
              4, 4,
              3, 3,
              2, 2,
              1, 1,
            }, new int[]{
              1, 1,
              2, 2,
              3, 3,
              4, 4,
              5, 5,
              6, 6,
            });
        });

        assertThrows(AssertionFailedError.class, () -> {
            assertClusteringEquals(bp, new int[]{
              5, 5,
              4, 4,
              3, 3,
              2, 2,
              1, 1,
            }, new int[]{
              4, 4,
              1, 1,
              1, 1,
              3, 3,
              5, 5,
            });
        });

        assertThrows(AssertionFailedError.class, () -> {
            assertClusteringEquals(bp, new int[]{
              5, 5,
              4, 4,
              3, 3,
              2, 2,
              1, 1,
            }, new int[]{
              1, 1,
              1, 2,
              1, 3,
              1, 4,
              1, 5,
            });
        });

    }
}
