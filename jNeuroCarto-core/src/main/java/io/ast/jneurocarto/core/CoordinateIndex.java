package io.ast.jneurocarto.core;

/**
 * A point using the coordinate system in the anatomical space.
 * It should always be used in global anatomical space.
 *
 * @param ap ap index
 * @param dv dv index
 * @param ml ml index
 */
public record CoordinateIndex(int ap, int dv, int ml) {

    /**
     * cast to coordinate.
     *
     * @param resolution resolution, assume three axis have same resolution.
     * @return index coordinate
     */
    public Coordinate toCoor(double resolution) {
        return new Coordinate(ap * resolution, dv * resolution, ml * resolution);
    }

    /**
     * cast to coordinate.
     *
     * @param resolution int array[(ap, dv, ml)]
     * @return coordinate
     * @throws IllegalArgumentException resolution not a 3-length array.
     */
    public Coordinate toCoor(double[] resolution) {
        if (resolution.length != 3) throw new IllegalArgumentException();
        return new Coordinate(ap * resolution[0], dv * resolution[1], ml * resolution[2]);
    }
}
