package io.ast.jneurocarto.core;

import org.jspecify.annotations.NullMarked;

/**
 * A point on a probe. Use {@code s}-th shank as its origin.
 *
 * @param s shank number
 * @param x x position
 * @param y y position
 * @param z z position
 */
@NullMarked
public record ProbeCoordinate(
    int s,
    double x,
    double y,
    double z
) {

    /**
     * Create a point on shank origin.
     *
     * @param s shank
     */
    public ProbeCoordinate(int s) {
        this(s, 0, 0, 0);
    }

    /**
     * Create a point on shank.
     *
     * @param s shank
     * @param x x position
     * @param y y position
     */
    public ProbeCoordinate(int s, double x, double y) {
        this(s, x, y, 0);
    }
}
