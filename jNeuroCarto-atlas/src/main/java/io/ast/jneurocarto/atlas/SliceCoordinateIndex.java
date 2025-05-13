package io.ast.jneurocarto.atlas;

/**
 * coordinate system in image volume space.
 *
 * @param p
 * @param x
 * @param y
 */
public record SliceCoordinateIndex(int p, int x, int y) {
    public SliceCoordinate toCoor(double resolution) {
        return new SliceCoordinate(p * resolution, x * resolution, y * resolution);
    }

    /**
     * @param resolution int array of {p, x, y}
     * @return
     */
    public SliceCoordinate toCoor(double[] resolution) {
        if (resolution.length != 3) throw new IllegalArgumentException();
        return new SliceCoordinate(p * resolution[0], x * resolution[1], y * resolution[2]);
    }
}
