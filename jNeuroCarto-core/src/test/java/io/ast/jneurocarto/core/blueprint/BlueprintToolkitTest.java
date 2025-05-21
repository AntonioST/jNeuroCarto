package io.ast.jneurocarto.core.blueprint;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class BlueprintToolkitTest {

    public static BlueprintToolkit<Object> fromShape(int ns, int ny, int nx) {
        return new BlueprintToolkit<>(BlueprintTest.fromShape(ns, ny, nx));
    }

    private static void assertBlueprintEquals(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
        try {
            assertArrayEquals(expect, actual);
        } catch (AssertionFailedError e) {
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

            throw new AssertionFailedError(message.toString(), e.getExpected(), e.getActual(), e);
        }
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


}
