package io.ast.jneurocarto.core.blueprint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class BlueprintToolkitTest {

    public static BlueprintToolkit<Object> fromShape(int ns, int ny, int nx) {
        return new BlueprintToolkit<>(BlueprintTest.fromShape(ns, ny, nx));
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
        assertArrayEquals(new int[]{
          1, 2,
          0, 1,
          0, 0,
          //---
          3, 4,
          0, 3,
          0, 0,
        }, bp.blueprint());

        bp.move(1);
        assertArrayEquals(new int[]{
          0, 0,
          1, 2,
          0, 1,
          //---
          0, 0,
          3, 4,
          0, 3,
        }, bp.blueprint());
    }


}
