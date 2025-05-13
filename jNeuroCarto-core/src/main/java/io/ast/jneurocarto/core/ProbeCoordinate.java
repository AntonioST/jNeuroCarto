package io.ast.jneurocarto.core;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ProbeCoordinate(
  int s,
  double x,
  double y,
  double z
) {

    public static final ProbeCoordinate ORIGIN = new ProbeCoordinate(0, 0, 0, 0);

    public ProbeCoordinate(int s) {
        this(s, 0, 0, 0);
    }

    public ProbeCoordinate(int s, double x, double y) {
        this(s, x, y, 0);
    }
}
