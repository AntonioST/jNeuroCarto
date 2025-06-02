package io.ast.jneurocarto.core;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ProbeTransform<C1, C2> {

    public interface Domain<C> {
        Domain<ProbeCoordinate> PROBE = new Probe();
        Domain<Coordinate> ANATOMICAL = new Anatomical();

        C fromPoint(Point3D p);

        Point3D toPoint(C coordinate);
    }


    public static class Probe implements Domain<ProbeCoordinate> {
        private Probe() {
        }

        @Override
        public ProbeCoordinate fromPoint(Point3D p) {
            return new ProbeCoordinate(0, p.getX(), p.getY(), p.getZ());
        }

        @Override
        public Point3D toPoint(ProbeCoordinate coordinate) {
            return new Point3D(coordinate.x(), coordinate.y(), coordinate.z());
        }
    }

    /**
     * A global anatomical space, used by a volume image.
     */
    public static class Anatomical implements Domain<Coordinate> {
        private Anatomical() {
        }

        @Override
        public Coordinate fromPoint(Point3D p) {
            return new Coordinate(p.getX(), p.getY(), p.getZ());
        }

        @Override
        public Point3D toPoint(Coordinate coordinate) {
            return new Point3D(coordinate.ap(), coordinate.dv(), coordinate.ml());
        }
    }

    /**
     * A referenced anatomical space, used when doing the implant surgery.
     *
     * @param reference the name of the reference.
     * @param origin    the origin of the reference in the global anatomical space.
     * @param flipAP    flip the AP-axis
     */
    public record ReferencedAnatomical(String reference, Coordinate origin, boolean flipAP) implements Domain<Coordinate> {

        @Override
        public Coordinate fromPoint(Point3D p) {
            return new Coordinate(p.getX(), p.getY(), p.getZ());
        }

        @Override
        public Point3D toPoint(Coordinate coordinate) {
            return new Point3D(coordinate.ap(), coordinate.dv(), coordinate.ml());
        }
    }


    private static final Point3D AXIS_AP = new Point3D(1, 0, 0);
    private static final Point3D AXIS_DV = new Point3D(0, 1, 0);
    private static final Point3D AXIS_ML = new Point3D(0, 0, 1);

    private final Domain<C1> d1;
    private final Domain<C2> d2;

    /**
     * Transformation from {@link ProbeCoordinate} to {@link Coordinate}.
     */
    private Affine transform;

    /**
     * Transformation from {@link Coordinate} to {@link ProbeCoordinate}
     */
    private Affine inverse;

    private ProbeTransform(Domain<C1> d1, Domain<C2> d2, Affine transform) {
        this.d1 = d1;
        this.d2 = d2;
        this.transform = transform;

        try {
            this.inverse = transform.createInverse();
        } catch (NonInvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    private ProbeTransform(Domain<C1> d1, Domain<C2> d2, Affine transform, Affine inverse) {
        this.d1 = d1;
        this.d2 = d2;
        this.transform = transform;
        this.inverse = inverse;
    }

    public Domain<C1> sourceDomain() {
        return d1;
    }

    public Domain<C2> targetDomain() {
        return d2;
    }

    /**
     * {@return a copied transform}
     */
    public Affine getTransform() {
        return new Affine(transform);
    }

    public void setTransform(Affine transform) {
        this.transform = transform;

        try {
            this.inverse = transform.createInverse();
        } catch (NonInvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    public ProbeTransform<C2, C1> inversed() {
        return new ProbeTransform<>(d2, d1, inverse, transform);
    }

    public <C3> ProbeTransform<C1, C3> compose(Domain<C3> domain, Affine transform) {
        var t = new Affine(this.transform);
        t.append(transform);
        return new ProbeTransform<>(this.d1, domain, t);
    }

    public <C3> ProbeTransform<C1, C3> compose(ProbeTransform<C2, C3> transform) {
        var t = new Affine(this.transform);
        t.append(transform.transform);
        return new ProbeTransform<>(this.d1, transform.d2, t);
    }

    /*======================*
     * point transformation *
     *======================*/

    /**
     * @param x x position (um) in C1 space.
     * @param y y position (um) in C1 space.
     * @return point in C2 space.
     */
    public Point3D transform(double x, double y) {
        return transform.transform(x, y, 0);
    }

    /**
     * @param x x position (um) in C1 space.
     * @param y y position (um) in C1 space.
     * @param z z position (um) in C1 space.
     * @return point in C2 space.
     */
    public Point3D transform(double x, double y, double z) {
        return transform.transform(x, y, z);
    }

    /**
     * @param p point in C1 coordinate.
     * @return point in C2 space.
     */
    public Point3D transform(Point2D p) {
        return transform.transform(p.getX(), p.getY(), 0);
    }

    /**
     * @param p point in C1 coordinate.
     * @return point in C2 space.
     */
    public Point3D transform(Point3D p) {
        return transform.transform(p);
    }

    /**
     * @param coordinate coordinate in C1 coordinate.
     * @return coordinate in C2 space.
     */
    public C2 transform(C1 coordinate) {
        return d2.fromPoint(transform.transform(d1.toPoint(coordinate)));
    }

    /**
     * @param ap ap position (um) in anatomical space.
     * @param dv dv position (um) in anatomical space.
     * @param ml ml position (um) in anatomical space.
     * @return point in probe coordinate.
     */
    public Point3D inverseTransform(double ap, double dv, double ml) {
        return inverse.transform(ap, dv, ml);
    }

    /**
     * @param p point in anatomical space.
     * @return point in probe coordinate.
     */
    public Point3D inverseTransform(Point3D p) {
        return inverse.transform(p);
    }

    /**
     * @param coordinate coordinate in C2 coordinate.
     * @return coordinate in C1 space.
     */
    public C1 inverseTransform(C2 coordinate) {
        return d1.fromPoint(inverse.transform(d2.toPoint(coordinate)));
    }

    /*===========*
     * factories *
     *===========*/

    /**
     * Create a coordinate transformation from {@link ProbeCoordinate} to {@link Coordinate} which
     * the {@code transform} satisfy
     * {@snippet lang = "java":
     * ProbeCoordinate pc = new ProbeCoordinate(0, 1, 1, 1); // @replace substring="1, 1, 1" replacement="..."
     * // 1. transformation
     * ProbeCoordinate cr = new Coordinate(transform.transform(pc.toPoint()));
     * // 2. revert transformation
     * pc.equals(new ProbeCoordinate(0, transform.inverseTransform(cr.toPoint())));
     *}
     *
     * @param transform transformation from {@link ProbeCoordinate} to {@link Coordinate}
     * @return
     */
    public static ProbeTransform<ProbeCoordinate, Coordinate> create(Affine transform) {
        return new ProbeTransform<>(Domain.PROBE, Domain.ANATOMICAL, transform);
    }

    /**
     * Create a coordinate transformation from {@link Coordinate} to a referenced {@link Coordinate}.
     *
     * @param reference the name of the reference.
     * @param origin    the origin of the reference in the global anatomical space.
     * @param flipAP    flip the AP-axis
     * @return
     */
    public static ProbeTransform<Coordinate, Coordinate> create(String reference, Coordinate origin, boolean flipAP) {
        var t = new Affine();
        if (flipAP) {
            t.append(new Affine(
                -1, 0, 0, origin.ap(), //
                0, 1, 0, -origin.dv(), //
                0, 0, -1, origin.ml() //
            ));
        } else {
            t.appendTranslation(-origin.ap(), -origin.dv(), -origin.ml());
        }
        return new ProbeTransform<>(Domain.ANATOMICAL, new ReferencedAnatomical(reference, origin, flipAP), t);
    }

    /**
     * Create a coordinate transform based on the {@code implant}.
     *
     * @param implant
     * @return
     */
    public static ProbeTransform<ProbeCoordinate, Coordinate> create(ImplantCoordinate implant) {
        var a = new Point3D(implant.ap(), implant.dv(), implant.ml());
        var t = new Affine();
        t.appendTranslation(implant.ap(), implant.dv() + implant.depth(), implant.ml());
        t.appendRotation(implant.rap(), a, AXIS_AP);
        t.appendRotation(implant.rdv(), a, AXIS_DV);
        t.appendRotation(implant.rml(), a, AXIS_ML);
        return create(t);
    }
}
