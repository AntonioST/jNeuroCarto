package io.ast.jneurocarto.atlas;

import org.junit.jupiter.api.Test;

import io.ast.jneurocarto.core.CoordinateIndex;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImageSlice {

    public static final int[] VOLUME = new int[]{100, 100, 100};

    @Test
    public void projectOnCoronal() {
        var slices = new ImageSliceStack(25, VOLUME, ImageSliceStack.Projection.coronal);
        var actual = slices.project(new CoordinateIndex(10, 20, 30));
        assertEquals(new SliceCoordinateIndex(10, 30, 20), actual);
    }

    @Test
    public void projectOnSagittal() {
        var slices = new ImageSliceStack(25, VOLUME, ImageSliceStack.Projection.sagittal);
        var actual = slices.project(new CoordinateIndex(10, 20, 30));
        assertEquals(new SliceCoordinateIndex(30, 10, 20), actual);
    }

    @Test
    public void projectOnTransverse() {
        var slices = new ImageSliceStack(25, VOLUME, ImageSliceStack.Projection.transverse);
        var actual = slices.project(new CoordinateIndex(10, 20, 30));
        assertEquals(new SliceCoordinateIndex(20, 30, 10), actual);
    }
}
