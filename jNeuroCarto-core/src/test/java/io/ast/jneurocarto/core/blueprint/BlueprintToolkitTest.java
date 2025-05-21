package io.ast.jneurocarto.core.blueprint;

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
            var message = buildBlueprintComparingMessage(bp, expect, actual);

            throw new AssertionFailedError(message.toString(), e.getExpected(), e.getActual(), e);
        }
    }

    private static void assertClusteringEquals(BlueprintToolkit<?> bp, int[] expect, Clustering actual) {
        assertClusteringEquals(bp, new Clustering(expect), actual);
    }

    private static void assertClusteringEquals(BlueprintToolkit<?> bp, Clustering expect, Clustering actual) {
        try {
            assertEquals(expect.length(), actual.length(), "different length");
            assertEquals(expect.groupNumber(), actual.groupNumber(), "different group number");
            var tmp = new Clustering(actual);
            for (var expGroup : expect.groups()) {
                var index = expect.indexGroup(expGroup);
                assertTrue(tmp.get(index) > 0);
                tmp.set(index, 0);
            }
            assertEquals(0, tmp.groupNumber());

        } catch (AssertionFailedError e) {
            var message = buildBlueprintComparingMessage(bp, expect.clustering(), actual.clustering());

            throw new AssertionFailedError(message.toString(), e.getExpected(), e.getActual(), e);

        } catch (Throwable e) {
            var message = buildBlueprintComparingMessage(bp, expect.clustering(), actual.clustering());

            throw new AssertionFailedError(message.toString(), expect, actual, e);
        }
    }

    private static StringBuilder buildBlueprintComparingMessage(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
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
        return message;
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


    }

}
