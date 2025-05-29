package io.ast.jneurocarto.core.blueprint;

import org.junit.jupiter.api.Test;

import io.ast.jneurocarto.core.ProbeDescription;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlueprintTest {

    public static Blueprint<Object> fromShape(int ns, int ny, int nx) {
        return BlueprintToolkit.dummy(ns, ny, nx).blueprint;
    }

    @Test
    public void fromShape142() {
        var bp = fromShape(1, 4, 2);
        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0, 0, 0}, bp.shank);
        assertArrayEquals(new int[]{0, 1, 0, 1, 0, 1, 0, 1}, bp.posx);
        assertArrayEquals(new int[]{0, 0, 1, 1, 2, 2, 3, 3}, bp.posy);
        assertEquals(1, bp.dx);
        assertEquals(1, bp.dy);
    }

    @Test
    public void fromShape222() {
        var bp = fromShape(2, 2, 2);
        assertArrayEquals(new int[]{0, 0, 0, 0, 1, 1, 1, 1}, bp.shank);
        assertArrayEquals(new int[]{0, 1, 0, 1, 0, 1, 0, 1}, bp.posx);
        assertArrayEquals(new int[]{0, 0, 1, 1, 0, 0, 1, 1}, bp.posy);
        assertEquals(1, bp.dx);
        assertEquals(1, bp.dy);
    }

    @Test
    public void setCate() {
        var bp = fromShape(1, 4, 2);

        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0, 0, 0}, bp.blueprint);

        var x = ProbeDescription.CATE_SET;
        bp.set(x);
        assertArrayEquals(new int[]{x, x, x, x, x, x, x, x}, bp.blueprint);
    }

    @Test
    public void setCateOnShank() {
        var bp = fromShape(2, 2, 2);

        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0, 0, 0}, bp.blueprint);

        var x = ProbeDescription.CATE_SET;
        bp.set(x, it -> it.s() == 1);
        assertArrayEquals(new int[]{0, 0, 0, 0, x, x, x, x}, bp.blueprint);
    }

    @Test
    public void unsetCate() {
        var bp = fromShape(2, 2, 2);

        var x = ProbeDescription.CATE_SET;
        bp.set(x);
        assertArrayEquals(new int[]{x, x, x, x, x, x, x, x}, bp.blueprint);

        bp.unset(it -> it.s() == 0);
        assertArrayEquals(new int[]{0, 0, 0, 0, x, x, x, x}, bp.blueprint);
    }

    @Test
    public void merge() {
        var b1 = fromShape(1, 4, 2);
        var b2 = fromShape(1, 4, 2);

        var x = ProbeDescription.CATE_SET;
        b1.set(x, it -> it.x() == 1);

        for (int i = 0, length = b2.blueprint.length; i < length; i++) {
            b2.blueprint[i] = i;
        }

        assertArrayEquals(new int[]{0, x, 0, x, 0, x, 0, x}, b1.blueprint);
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5, 6, 7}, b2.blueprint);
        assertArrayEquals(new int[]{0, x, 2, x, 4, x, 6, x}, b1.merge(b2).blueprint);

    }
}
