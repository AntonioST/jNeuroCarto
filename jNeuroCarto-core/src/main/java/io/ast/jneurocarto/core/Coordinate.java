package io.ast.jneurocarto.core;

/**
 * A point using the coordinate system in the anatomical space,
 * either global or referenced space.
 *
 * @param ap um
 * @param dv um
 * @param ml um
 */
public record Coordinate(double ap, double dv, double ml) {

    /**
     * cast to index coordinate.
     *
     * @param resolution resolution, assume three axis have same resolution.
     * @return index coordinate
     */
    public CoordinateIndex toCoorIndex(double resolution) {
        return new CoordinateIndex((int) (ap / resolution), (int) (dv / resolution), (int) (ml / resolution));
    }

    /**
     * cast to index coordinate.
     *
     * @param resolution int array[(ap, dv, ml)]
     * @return index coordinate
     * @throws IllegalArgumentException resolution not a 3-length array.
     */
    public CoordinateIndex toCoorIndex(double[] resolution) {
        if (resolution.length != 3) throw new IllegalArgumentException();
        return new CoordinateIndex((int) (ap / resolution[0]), (int) (dv / resolution[1]), (int) (ml / resolution[2]));
    }
}
