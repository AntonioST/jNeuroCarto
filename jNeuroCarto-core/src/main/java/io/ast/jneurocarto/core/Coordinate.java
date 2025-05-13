package io.ast.jneurocarto.core;

/**
 * coordinate system in anatomical space.
 *
 * @param ap um
 * @param dv um
 * @param ml um
 */
public record Coordinate(double ap, double dv, double ml) {
    public CoordinateIndex toCoorIndex(double resolution) {
        return new CoordinateIndex((int) (ap / resolution), (int) (dv / resolution), (int) (ml / resolution));
    }

    /**
     * @param resolution int array of {ap, dv, ml}
     * @return
     */
    public CoordinateIndex toCoorIndex(double[] resolution) {
        if (resolution.length != 3) throw new IllegalArgumentException();
        return new CoordinateIndex((int) (ap / resolution[0]), (int) (dv / resolution[1]), (int) (ml / resolution[2]));
    }
}
