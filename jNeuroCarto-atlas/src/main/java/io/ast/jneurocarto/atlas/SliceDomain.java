package io.ast.jneurocarto.atlas;

import javafx.geometry.Point3D;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ProbeTransform;

@NullMarked
public class SliceDomain implements ProbeTransform.Domain<SliceCoordinate> {

    public static final SliceDomain INSTANCE = new SliceDomain();

    private SliceDomain() {
    }

    @Override
    public Point3D toPoint(SliceCoordinate coordinate) {
        return new Point3D(coordinate.x(), coordinate.y(), coordinate.p());
    }

    @Override
    public SliceCoordinate fromPoint(Point3D p) {
        return new SliceCoordinate(p.getZ(), p.getX(), p.getY());
    }
}
