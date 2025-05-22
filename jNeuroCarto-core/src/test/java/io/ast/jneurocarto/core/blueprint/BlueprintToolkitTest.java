package io.ast.jneurocarto.core.blueprint;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

public class BlueprintToolkitTest {

    public static BlueprintToolkit<Object> fromShape(int ns, int ny, int nx) {
        return new BlueprintToolkit<>(BlueprintTest.fromShape(ns, ny, nx));
    }

    private static void assertBlueprintEquals(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
        try {
            assertArrayEquals(expect, actual);
        } catch (AssertionFailedError e) {
            String message;

            try {
                message = buildBlueprintComparingMessage(bp, expect, actual);
            } catch (Exception ex) {
                throw e;
            }

            throw new AssertionFailedError(message, e.getExpected(), e.getActual(), e);
        }
    }

    private static void assertClusteringEquals(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
        assertClusteringEquals(bp, new Clustering(expect), new Clustering(actual));
    }

    private static void assertClusteringEquals(BlueprintToolkit<?> bp, int[] expect, Clustering actual) {
        assertClusteringEquals(bp, new Clustering(expect), actual);
    }

    private static void assertClusteringEquals(BlueprintToolkit<?> bp, Clustering expect, Clustering actual) {
        try {
            assertEquals(expect.length(), actual.length(), "different length");

            var totalGroup = expect.groupNumber();
            var actualGroup = actual.groupNumber();
            assertEquals(totalGroup, actualGroup, "different group number");

            var tmp = new Clustering(actual);
            for (var expGroup : expect.groups()) {
                var index = expect.indexGroup(expGroup);
                var actGroup = assertDoesNotThrow(() -> tmp.get(index), "not unique group with given index");
                assertTrue(actGroup > 0, "not a group with given index");
                tmp.set(index, 0);
                assertEquals(--actualGroup, tmp.groupNumber(), "group does not match with given index");
            }
            assertEquals(0, tmp.groupNumber(), "has extra groups");

        } catch (AssertionFailedError e) {
            String message;
            try {
                message = buildBlueprintComparingMessage(bp, expect.clustering(), actual.clustering());
            } catch (Exception ex) {
                throw e;
            }

            throw new AssertionFailedError(message, e.getExpected(), e.getActual(), e);

        } catch (Throwable e) {
            String message;
            try {
                message = buildBlueprintComparingMessage(bp, expect.clustering(), actual.clustering());
            } catch (Exception ex) {
                throw e;
            }

            throw new AssertionFailedError(message, expect, actual, e);
        }
    }

    private static String buildBlueprintComparingMessage(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
        var expIter = bp.toStringBlueprint(expect).lines().iterator();
        var actIter = bp.toStringBlueprint(actual).lines().iterator();
        var expLen = 0;
        var actLen = 0;
        String expText;
        String actText;

        var message = new StringBuilder("\n");

        while (expIter.hasNext() && actIter.hasNext()) {
            expText = expIter.next();
            actText = actIter.next();
            expLen = expText.length();
            actLen = actText.length();
            message.append(expText).append("  ").append(actText).append('\n');
        }

        if (expIter.hasNext()) {
            actText = " ".repeat(actLen);
            while (expIter.hasNext()) {
                message.append(expIter.next()).append("  ").append(actText).append('\n');
            }
        } else if (actIter.hasNext()) {
            expText = " ".repeat(expLen);
            while (actIter.hasNext()) {
                message.append(expText).append("  ").append(actIter.next()).append('\n');
            }
        }

        message.append("%%-%ds".formatted(expLen).formatted("exp")).append("  ")
          .append("%%-%ds".formatted(actLen).formatted("act")).append('\n');

        return message.toString();
    }


    @Test
    @Order(0)
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

    @Test
    public void move() {
        var bp = fromShape(2, 3, 2);
        bp.setBlueprint(new int[]{
          2, 0,
          1, 2,
          0, 1,
          //---
          4, 0,
          3, 4,
          0, 3,
        });

        bp.move(-1);
        assertBlueprintEquals(bp, new int[]{
          1, 2,
          0, 1,
          0, 0,
          //---
          3, 4,
          0, 3,
          0, 0,
        }, bp.blueprint());

        bp.move(1);
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          1, 2,
          0, 1,
          //---
          0, 0,
          3, 4,
          0, 3,
        }, bp.blueprint());
    }

    @Test
    public void moveBlueprint() {
        var bp = fromShape(2, 3, 2);
        var blueprint = new int[]{
          2, 0,
          1, 2,
          0, 1,
          //---
          4, 0,
          3, 4,
          0, 3,
        };
        var o = blueprint.clone();

        assertBlueprintEquals(bp, new int[]{
          1, 2,
          0, 1,
          0, 0,
          //---
          3, 4,
          0, 3,
          0, 0,
        }, bp.move(blueprint, -1));
        assertArrayEquals(o, blueprint);

        assertBlueprintEquals(bp, new int[]{
          0, 0,
          2, 0,
          1, 2,
          //---
          0, 0,
          4, 0,
          3, 4,
        }, bp.move(blueprint, 1));
        assertArrayEquals(o, blueprint);
    }

    @Test
    public void noMoveBlueprint() {
        var bp = fromShape(2, 3, 2);
        var blueprint = new int[]{
          2, 0,
          1, 2,
          0, 1,
          //---
          4, 0,
          3, 4,
          0, 3,
        };
        var o = blueprint.clone();

        assertBlueprintEquals(bp, new int[]{
          2, 0,
          1, 2,
          0, 1,
          //---
          4, 0,
          3, 4,
          0, 3,
        }, bp.move(blueprint, 0));
        assertArrayEquals(o, blueprint);
    }

    @Test
    public void moveBlueprintCategory() {
        var bp = fromShape(2, 3, 2);
        var blueprint = new int[]{
          2, 0,
          1, 2,
          0, 1,
          //---
          4, 0,
          3, 4,
          0, 3,
        };
        var o = blueprint.clone();

        assertBlueprintEquals(bp, new int[]{
          0, 2,
          1, 0,
          0, 1,
          //---
          4, 0,
          3, 4,
          0, 3,
        }, bp.move(blueprint, -1, 2));
        assertArrayEquals(o, blueprint);

        assertBlueprintEquals(bp, new int[]{
          0, 0,
          2, 0,
          0, 2,
          //---
          4, 0,
          3, 4,
          0, 3,
        }, bp.move(blueprint, 1, 2));
        assertArrayEquals(o, blueprint);

        assertBlueprintEquals(bp, new int[]{
          2, 0,
          0, 2,
          1, 0,
          //---
          4, 0,
          3, 4,
          0, 3,
        }, bp.move(blueprint, 1, 1));
        assertArrayEquals(o, blueprint);
    }


    @Test
    public void clustering() {
        var bp = fromShape(1, 5, 2);
        assertClusteringEquals(bp, new int[]{
          1, 1,
          0, 1,
          1, 1,
          0, 1,
          1, 1,
        }, bp.findClustering(new int[]{
          1, 1,
          0, 1,
          1, 1,
          0, 1,
          1, 1,
        }, false));

        assertClusteringEquals(bp, new int[]{
          1, 1,
          0, 1,
          2, 2,
          0, 3,
          3, 3,
        }, bp.findClustering(new int[]{
          1, 1,
          0, 1,
          2, 2,
          0, 1,
          1, 1,
        }, false));
    }

    @Test
    public void clusteringWithDiagonal() {
        var bp = fromShape(1, 5, 2);
        assertClusteringEquals(bp, new int[]{
          1, 1,
          0, 1,
          2, 0,
          0, 3,
          3, 3,
        }, bp.findClustering(new int[]{
          1, 1,
          0, 1,
          1, 0,
          0, 1,
          1, 1,
        }, false));

        assertClusteringEquals(bp, new int[]{
          1, 1,
          0, 1,
          1, 0,
          0, 1,
          1, 1,
        }, bp.findClustering(new int[]{
          1, 1,
          0, 1,
          1, 0,
          0, 1,
          1, 1,
        }, true));

        assertClusteringEquals(bp, new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        }, bp.findClustering(new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        }, true));
    }

    @Test
    public void clusteringCategory() {
        var bp = fromShape(1, 5, 2);
        assertClusteringEquals(bp, new int[]{
          1, 1,
          0, 1,
          1, 1,
          0, 1,
          1, 1,
        }, bp.findClustering(new int[]{
          1, 1,
          0, 1,
          1, 1,
          0, 1,
          1, 1,
        }, 1, false));

        assertClusteringEquals(bp, new int[]{
          1, 1,
          0, 1,
          0, 0,
          0, 2,
          2, 2,
        }, bp.findClustering(new int[]{
          1, 1,
          0, 1,
          2, 2,
          0, 1,
          1, 1,
        }, 1, false));

        assertClusteringEquals(bp, new int[]{
          1, 1,
          0, 1,
          1, 0,
          0, 1,
          1, 1,
        }, bp.findClustering(new int[]{
          1, 1,
          0, 1,
          1, 0,
          0, 1,
          1, 1,
        }, 1, true));
    }

    @Test
    public void fill() {
        var bp = fromShape(1, 5, 2);
        bp.setBlueprint(new int[]{
          1, 1,
          0, 1,
          1, 1,
          0, 1,
          1, 1,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
        }, bp.blueprint());

        bp.setBlueprint(new int[]{
          0, 1,
          1, 1,
          1, 1,
          1, 1,
          1, 0,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
        }, bp.blueprint());

        bp.setBlueprint(new int[]{
          0, 0,
          0, 1,
          1, 1,
          1, 0,
          0, 0,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        }, bp.blueprint());

        bp.setBlueprint(new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        }, bp.blueprint());
    }

    @Test
    public void fillWithThreshold() {
        var bp = fromShape(1, 5, 2);
        bp.setBlueprint(new int[]{
          2, 2,
          2, 2,
          2, 0,
          0, 1,
          1, 1,
        });

        bp.fill(new BlueprintToolkit.AreaThreshold(4, 10));
        assertBlueprintEquals(bp, new int[]{
          2, 2,
          2, 2,
          2, 2,
          0, 1,
          1, 1,
        }, bp.blueprint());

        bp.setBlueprint(new int[]{
          2, 2,
          2, 2,
          2, 0,
          0, 1,
          1, 1,
        });

        bp.fill(new BlueprintToolkit.AreaThreshold(0, 4));
        assertBlueprintEquals(bp, new int[]{
          2, 2,
          2, 2,
          2, 0,
          1, 1,
          1, 1,
        }, bp.blueprint());
    }

    @Test
    public void extend() {
        var bp = fromShape(1, 5, 2);
        bp.setBlueprint(new int[]{
          0, 0,
          0, 0,
          1, 1,
          0, 0,
          0, 0,
        });

        bp.extend(1, 1);
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        }, bp.blueprint());
    }

    @Test
    public void extendWithValue() {
        var bp = fromShape(1, 5, 2);
        bp.setBlueprint(new int[]{
          0, 0,
          0, 0,
          1, 1,
          0, 0,
          0, 0,
        });

        bp.extend(1, 1, 2);
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          2, 2,
          1, 1,
          2, 2,
          0, 0,
        }, bp.blueprint());
    }

    @Test
    public void reduce() {
        var bp = fromShape(1, 7, 2);
        bp.setBlueprint(new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        });

        bp.reduce(1, 1);
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
          0, 0,
        }, bp.blueprint());

        bp.setBlueprint(new int[]{
          2, 2,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        });

        bp.reduce(1, 1);
        assertBlueprintEquals(bp, new int[]{
          2, 2,
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
          0, 0,
        }, bp.blueprint());
    }

    @Test
    public void reduceWithValue() {
        var bp = fromShape(1, 7, 2);
        bp.setBlueprint(new int[]{
          0, 0,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          1, 1,
          0, 0,
        });

        bp.reduce(1, 1, 2);
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          2, 2,
          1, 1,
          1, 1,
          1, 1,
          2, 2,
          0, 0,
        }, bp.blueprint());
    }

    @Test
    public void reduceToNone() {
        var bp = fromShape(1, 4, 2);
        bp.setBlueprint(new int[]{
          0, 0,
          1, 1,
          1, 1,
          0, 0,
        });

        bp.reduce(1, 1);
        assertBlueprintEquals(bp, new int[]{
          0, 0,
          0, 0,
          0, 0,
          0, 0,
        }, bp.blueprint());
    }

}
