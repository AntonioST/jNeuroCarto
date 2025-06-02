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
    public ProbeCoordinate(int s) {
        this(s, 0, 0, 0);
    }

    public ProbeCoordinate(int s, double x, double y) {
        this(s, x, y, 0);
    }
}
