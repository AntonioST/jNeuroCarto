package io.ast.jneurocarto.atlas;

import javafx.geometry.Point2D;

/**
 * coordinate system in image volume space.
 *
 * @param p um
 * @param x um
 * @param y um
 */
public record SliceCoordinate(double p, double x, double y) {

    public SliceCoordinate(double plane, Point2D point) {
        this(plane, point.getX(), point.getY());
    }

    public SliceCoordinateIndex toCoorIndex(double resolution) {
        return new SliceCoordinateIndex((int) (p / resolution), (int) (x / resolution), (int) (y / resolution));
    }

    /**
     * @param resolution int array of {p, x, y}
     * @return
     */
    public SliceCoordinateIndex toCoorIndex(double[] resolution) {
        if (resolution.length != 3) throw new IllegalArgumentException();
        return new SliceCoordinateIndex((int) (p / resolution[0]), (int) (x / resolution[1]), (int) (y / resolution[2]));
    }
}
