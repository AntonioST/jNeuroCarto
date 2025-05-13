package io.ast.jneurocarto.atlas;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImageSlice {

    public static final int[] VOLUME = new int[]{100, 100, 100};

    @Test
    public void projectOnCoronal() {
        var slices = new ImageSlices(25, VOLUME, ImageSlices.Projection.coronal);
        var actual = slices.project(new BrainAtlas.CoordinateIndex(10, 20, 30));
        assertEquals(new ImageSlices.CoordinateIndex(10, 30, 20), actual);
    }

    @Test
    public void projectOnSagittal() {
        var slices = new ImageSlices(25, VOLUME, ImageSlices.Projection.sagittal);
        var actual = slices.project(new BrainAtlas.CoordinateIndex(10, 20, 30));
        assertEquals(new ImageSlices.CoordinateIndex(30, 10, 20), actual);
    }

    @Test
    public void projectOnTransverse() {
        var slices = new ImageSlices(25, VOLUME, ImageSlices.Projection.transverse);
        var actual = slices.project(new BrainAtlas.CoordinateIndex(10, 20, 30));
        assertEquals(new ImageSlices.CoordinateIndex(20, 30, 10), actual);
    }
}
