package io.ast.jneurocarto.javafx.chart.data;

import javafx.geometry.Point2D;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class XY {
    static final XY GAP = new XY(Double.NaN, Double.NaN, 0.0, null);

    double x;
    double y;
    double v;
    @Nullable
    Object external;

    public XY(Point2D p) {
        this(p.getX(), p.getY(), 0, null);
    }

    public XY(Point2D p, double v) {
        this(p.getX(), p.getY(), v, null);
    }

    public XY(double x, double y) {
        this(x, y, 0, null);
    }

    public XY(double x, double y, double v) {
        this(x, y, v, null);
    }

    public XY(Point2D p, @Nullable Object external) {
        this(p.getX(), p.getY(), 0, external);
    }

    public XY(Point2D p, double v, @Nullable Object external) {
        this(p.getX(), p.getY(), v, external);
    }

    public XY(double x, double y, @Nullable Object external) {
        this(x, y, 0, external);
    }

    public XY(double x, double y, double v, @Nullable Object external) {
        this.x = x;
        this.y = y;
        this.v = v;
        this.external = external;
    }

    public double x() {
        return x;
    }

    public void x(double x) {
        this.x = x;
    }

    public double y() {
        return y;
    }

    public void y(double y) {
        this.y = y;
    }

    public double v() {
        return v;
    }

    public void v(double v) {
        this.v = v;
    }

    public boolean isGap() {
        return Double.isNaN(x) || Double.isNaN(y);
    }

    public @Nullable Object external() {
        return external;
    }

    public void external(Object external) {
        this.external = external;
    }

    @Override
    public String toString() {
        return "XY{" + x + "," + y + '}';
    }
}
