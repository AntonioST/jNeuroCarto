package io.ast.jneurocarto.core;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;

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

    public ProbeCoordinate(int s, Point2D p) {
        this(s, p.getX(), p.getY(), 0);
    }

    public ProbeCoordinate(int s, Point3D p) {
        this(s, p.getX(), p.getY(), p.getZ());
    }

    public ProbeCoordinate offset(double x, double y, double z) {
        return new ProbeCoordinate(s, this.x + x, this.y + y, this.z + z);
    }

    public ProbeCoordinate offset(ProbeCoordinate offset) {
        return new ProbeCoordinate(s, offset.x + x, offset.y + y, offset.z + z);
    }
}
