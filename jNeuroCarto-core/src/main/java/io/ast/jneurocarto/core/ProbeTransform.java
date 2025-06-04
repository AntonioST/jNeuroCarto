package io.ast.jneurocarto.core;

import java.util.function.Supplier;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed abstract class ProbeTransform<C1, C2> {

    public interface Domain<C> {
        C fromPoint(Point3D p);

        Point3D toPoint(C coordinate);
    }

    /**
     * A chart space, used by a probe coordinate
     */
    public static final Domain<ProbeCoordinate> PROBE = new Probe();

    /**
     * A global anatomical space, used by a volume image.
     */
    public static final Domain<Coordinate> ANATOMICAL = new Anatomical();

    public record Project2D(String name) implements Domain<Point2D> {
        @Override
        public Point2D fromPoint(Point3D p) {
            return new Point2D(p.getX(), p.getY());
        }

        @Override
        public Point3D toPoint(Point2D coordinate) {
            return new Point3D(coordinate.getX(), coordinate.getY(), 0);
        }
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

    protected final Domain<C1> d1;
    protected final Domain<C2> d2;

    protected ProbeTransform(Domain<C1> d1, Domain<C2> d2) {
        this.d1 = d1;
        this.d2 = d2;
    }

    /**
     * Transformation from {@link #d1} to {@link #d2}.
     */
    protected abstract Affine transform();

    /**
     * Transformation from {@link #d2} to {@link #d1}
     */
    protected abstract Affine inverse();

    public abstract ProbeTransform<C2, C1> inverted();


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
        return new Affine(transform());
    }

    public abstract <C3> ProbeTransform<C1, C3> compose(Domain<C3> domain, Affine transform);

    public abstract <C3> ProbeTransform<C1, C3> compose(ProbeTransform<C2, C3> transform);

    protected void checkComposeDomain(ProbeTransform<?, ?> transform) {
        if (!targetDomain().equals(transform.sourceDomain())) {
            throw new RuntimeException("domain mismatch between " + targetDomain() + " and " + transform.sourceDomain());
        }
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
        return transform().transform(x, y, 0);
    }

    /**
     * @param x x position (um) in C1 space.
     * @param y y position (um) in C1 space.
     * @param z z position (um) in C1 space.
     * @return point in C2 space.
     */
    public Point3D transform(double x, double y, double z) {
        return transform().transform(x, y, z);
    }

    /**
     * @param p point in C1 coordinate.
     * @return point in C2 space.
     */
    public Point3D transform(Point2D p) {
        return transform().transform(p.getX(), p.getY(), 0);
    }

    /**
     * @param p point in C1 coordinate.
     * @return point in C2 space.
     */
    public Point3D transform(Point3D p) {
        return transform().transform(p);
    }

    /**
     * @param coordinate coordinate in C1 coordinate.
     * @return coordinate in C2 space.
     */
    public C2 transform(C1 coordinate) {
        return d2.fromPoint(transform().transform(d1.toPoint(coordinate)));
    }

    /**
     * @param ap ap position (um) in anatomical space.
     * @param dv dv position (um) in anatomical space.
     * @param ml ml position (um) in anatomical space.
     * @return point in probe coordinate.
     */
    public Point3D inverseTransform(double ap, double dv, double ml) {
        return inverse().transform(ap, dv, ml);
    }

    /**
     * @param p point in anatomical space.
     * @return point in probe coordinate.
     */
    public Point3D inverseTransform(Point3D p) {
        return inverse().transform(p);
    }

    /**
     * @param coordinate coordinate in C2 coordinate.
     * @return coordinate in C1 space.
     */
    public C1 inverseTransform(C2 coordinate) {
        return d1.fromPoint(inverse().transform(d2.toPoint(coordinate)));
    }

    public Point3D deltaTransform(double x, double y) {
        return transform().deltaTransform(x, y, 0);
    }

    public Point3D deltaTransform(double x, double y, double z) {
        return transform().deltaTransform(x, y, z);
    }

    public Point3D deltaTransform(Point2D p) {
        return transform().deltaTransform(p.getX(), p.getY(), 0);
    }

    public Point3D deltaTransform(Point3D p) {
        return transform().deltaTransform(p);
    }

    /*===========*
     * factories *
     *===========*/

    public static <C> ProbeTransform<C, C> identify(Domain<C> domain) {
        return new FixedTransform<>(domain, domain, new Affine());
    }

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
        return new FixedTransform<>(PROBE, ANATOMICAL, transform);
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
        return new FixedTransform<>(ANATOMICAL, new ReferencedAnatomical(reference, origin, flipAP), t);
    }

    /**
     * Create a coordinate transform based on the {@code implant}. Do not consider implant reference.
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

    public static <C1, C2> ProbeTransform<C1, C2> create(Domain<C1> source, Domain<C2> target, Affine transform) {
        return new FixedTransform<>(source, target, transform);
    }

    public static <C1, C2> ProbeTransform<C1, C2> create(Domain<C1> source, Domain<C2> target, Supplier<Affine> transform) {
        return new DynamicTransform<>(source, target, transform, false);
    }

    /*===========*
     * implement *
     *===========*/

    private static final class FixedTransform<C1, C2> extends ProbeTransform<C1, C2> {
        private final Affine transform;
        private final Affine inverse;

        FixedTransform(Domain<C1> d1, Domain<C2> d2, Affine transform) {
            super(d1, d2);
            this.transform = transform;

            try {
                this.inverse = transform.createInverse();
            } catch (NonInvertibleTransformException e) {
                throw new RuntimeException(e);
            }
        }

        FixedTransform(Domain<C1> d1, Domain<C2> d2, Affine transform, Affine inverse) {
            super(d1, d2);
            this.transform = transform;
            this.inverse = inverse;
        }

        @Override
        protected Affine transform() {
            return transform;
        }

        @Override
        protected Affine inverse() {
            return inverse;
        }

        public ProbeTransform<C2, C1> inverted() {
            return new FixedTransform<>(d2, d1, inverse, transform);
        }

        public <C3> ProbeTransform<C1, C3> compose(Domain<C3> domain, Affine transform) {
            var t = new Affine(transform);
            t.append(transform);
            return new FixedTransform<>(this.d1, domain, t);
        }

        public <C3> ProbeTransform<C1, C3> compose(ProbeTransform<C2, C3> transform) {
            checkComposeDomain(transform);
            if (transform instanceof ProbeTransform.FixedTransform<C2, C3> f) {
                var t = new Affine(this.transform);
                t.append(f.transform());
                return new FixedTransform<>(this.d1, transform.d2, t);
            } else if (transform instanceof ProbeTransform.ComposedTransform<C2, ?, C3> f) {
                return compose(f);
            } else {
                return new ComposedTransform<>(this, transform);
            }
        }

        public <C3, C4> ProbeTransform<C1, C4> compose(ComposedTransform<C2, C3, C4> transform) {
            if (transform.f instanceof ProbeTransform.FixedTransform<C2, C3> f) {
                return new ComposedTransform<>(compose(f), transform.g);
            } else {
                return new ComposedTransform<>(this, transform);
            }
        }
    }

    private static final class DynamicTransform<C1, C2> extends ProbeTransform<C1, C2> {
        private final Supplier<Affine> transform;
        private final boolean inverted;

        DynamicTransform(Domain<C1> d1, Domain<C2> d2, Supplier<Affine> transform, boolean inverted) {
            super(d1, d2);
            this.transform = transform;
            this.inverted = inverted;
        }

        @Override
        protected Affine transform() {
            var t = transform.get();
            try {
                return inverted ? t.createInverse() : t;
            } catch (NonInvertibleTransformException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected Affine inverse() {
            var t = transform.get();
            try {
                return inverted ? t : t.createInverse();
            } catch (NonInvertibleTransformException e) {
                throw new RuntimeException(e);
            }
        }

        public ProbeTransform<C2, C1> inverted() {
            return new DynamicTransform<>(d2, d1, transform, !inverted);
        }

        @Override
        public <C3> ProbeTransform<C1, C3> compose(Domain<C3> domain, Affine transform) {
            return new ComposedTransform<>(this, new FixedTransform<>(targetDomain(), domain, transform));
        }

        @Override
        public <C3> ProbeTransform<C1, C3> compose(ProbeTransform<C2, C3> transform) {
            checkComposeDomain(transform);
            return new ComposedTransform<>(this, transform);
        }
    }

    private static final class ComposedTransform<C1, C2, C3> extends ProbeTransform<C1, C3> {
        private final ProbeTransform<C1, C2> f;
        private final ProbeTransform<C2, C3> g;

        ComposedTransform(ProbeTransform<C1, C2> f, ProbeTransform<C2, C3> g) {
            super(f.sourceDomain(), g.targetDomain());

            // f can not be ComposedTransform.
            if (f instanceof ProbeTransform.ComposedTransform<?, ?, ?>) {
                throw new RuntimeException();
            }

            this.f = f;
            this.g = g;
        }

        @Override
        protected Affine transform() {
            var t = new Affine(f.transform());
            t.append(g.transform());
            return t;
        }

        @Override
        protected Affine inverse() {
            var t = new Affine(g.inverse());
            t.append(f.inverse());
            return t;
        }

        /*
        ProbeTransform T = S | C
        F = FixedTransform F | DynamicTransform D
        C = ComposedTransform (S, T)
        illegal: ComposedTransform (F, F)
        illegal: ComposedTransform (F, (F, T))

        inverted(T)
        | S -> S'
        | (S1, S2) -> (S2', S1')
        | (S, C) -> compose(inverted(C) , S')
        e.g. inverted((S1, (S2, S3))) = (S3', (S2', S1'))
        e.g. inverted((S1, (S2, (... (Sn))))) = (Sn', (..., (S2', S1')))

        compose(T, T)
        | (F, F) -> F
        | (F, (F, C)) -> (compose(F, F), C)
        | (S, T) -> (S, T)
        | ((S, T1), T2) -> (S, compose(T1, T2))
        e.g. compose((S1, (S2, S3)), S4) = (S1, (S2, (S3, S4)))
         */

        @Override
        public ProbeTransform<C3, C1> inverted() {
            return inverted(f, g);
        }

        private static <C1, C2, C3> ProbeTransform<C3, C1> inverted(ProbeTransform<C1, C2> f, ProbeTransform<C2, C3> g) {
            assert !(f instanceof ProbeTransform.ComposedTransform<C1, ?, C2>);
            if (g instanceof ProbeTransform.ComposedTransform<C2, ?, C3> gg) {
                return composeInvert(gg, f.inverted());
            } else {
                return new ComposedTransform<>(g.inverted(), f.inverted());
            }
        }

        private static <C1, C2, C3, C4> ProbeTransform<C4, C1> composeInvert(ComposedTransform<C2, C3, C4> c, ProbeTransform<C2, C1> r) {
            var rr = new ComposedTransform<>(c.f.inverted(), r);

            if (c.g instanceof ProbeTransform.ComposedTransform<C3, ?, C4> cg) {
                return composeInvert(cg, rr);
            } else {
                return new ComposedTransform<>(c.g.inverted(), rr);
            }
        }

        @Override
        public <C4> ProbeTransform<C1, C4> compose(Domain<C4> domain, Affine transform) {
            return compose(new FixedTransform<>(targetDomain(), domain, transform));
        }

        @Override
        public <C4> ProbeTransform<C1, C4> compose(ProbeTransform<C3, C4> transform) {
            checkComposeDomain(transform);
            return new ComposedTransform<>(f, g.compose(transform));
        }
    }
}
