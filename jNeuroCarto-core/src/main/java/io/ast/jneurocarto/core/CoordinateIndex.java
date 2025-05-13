package io.ast.jneurocarto.core;

/**
 * coordinate system in anatomical space.
 *
 * @param ap
 * @param dv
 * @param ml
 */
public record CoordinateIndex(int ap, int dv, int ml) {
    public Coordinate toCoor(double resolution) {
        return new Coordinate(ap * resolution, dv * resolution, ml * resolution);
    }

    /**
     * @param resolution int array of {ap, dv, ml}
     * @return
     */
    public Coordinate toCoor(double[] resolution) {
        if (resolution.length != 3) throw new IllegalArgumentException();
        return new Coordinate(ap * resolution[0], dv * resolution[1], ml * resolution[2]);
    }
}
