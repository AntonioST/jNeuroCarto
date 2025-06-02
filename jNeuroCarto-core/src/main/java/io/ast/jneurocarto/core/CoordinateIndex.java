package io.ast.jneurocarto.core;

/**
 * A point using the coordinate system in the anatomical space.
 *
 * @param ap ap index
 * @param dv dv index
 * @param ml ml index
 */
public record CoordinateIndex(int ap, int dv, int ml) {
    public Coordinate toCoor(double resolution) {
        return new Coordinate(ap * resolution, dv * resolution, ml * resolution);
    }

    /**
     * @param resolution int array[(ap, dv, ml)]
     * @return
     */
    public Coordinate toCoor(double[] resolution) {
        if (resolution.length != 3) throw new IllegalArgumentException();
        return new Coordinate(ap * resolution[0], dv * resolution[1], ml * resolution[2]);
    }

    public CoordinateIndex offset(int ap, int dv, int ml) {
        return new CoordinateIndex(this.ap + ap, this.dv + dv, this.ml + ml);
    }

    public CoordinateIndex offset(CoordinateIndex offset) {
        return new CoordinateIndex(offset.ap + ap, offset.dv + dv, offset.ml + ml);
    }
}
