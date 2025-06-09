package io.ast.jneurocarto.core;

import java.util.function.Supplier;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;

/**
 * Transform between different 3D coordinate system.
 *
 * @param <C1> coordinate type from source domain
 * @param <C2> coordinate type from target domain.
 */
@NullMarked
public sealed abstract class ProbeTransform<C1, C2> {

    /**
     * coordinate domain.
     * <p>
     * Domain should implement {@link #equals(Object)} if necessary.
     *
     * @param <C> coordinate type
     */
    public interface Domain<C> {

        /**
         * cast {@link Point3D} into coordinate.
         *
         * @param p point
         * @return coordinate instance
         */
        C fromPoint(Point3D p);

        /**
         * cast coordinate into {@link Point3D}.
         *
         * @param coordinate coordinate instance
         * @return point
         */
        Point3D toPoint(C coordinate);
    }

    /**
     * A chart/probe space, used by {@link ProbeCoordinate}.
     */
    public static final Domain<ProbeCoordinate> PROBE = new Probe();

    /**
     * A global anatomical space, used by {@link Coordinate}.
     */
    public static final Domain<Coordinate> ANATOMICAL = new Anatomical();

    /**
     * A 2D projection domain. Just drop {@link Point3D#z}.
     *
     * @param name domain name.
     */
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

    /**
     * A chart/probe space, used by {@link ProbeCoordinate}.
     */
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
     * A global anatomical space, used by {@link Coordinate}.
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
     * @see #create(String, Coordinate, boolean)
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

    /**
     * source domain
     */
    protected final Domain<C1> source;

    /**
     * target domain
     */
    protected final Domain<C2> target;

    /**
     * Create a transform with given source and target domain.
     * The actual transform matrix is provided in sub-classes.
     *
     * @param source source domain
     * @param target target domain
     */
    ProbeTransform(Domain<C1> source, Domain<C2> target) {
        this.source = source;
        this.target = target;
    }

    /**
     * The transformation from source domain to target domain.
     */
    abstract Affine transform();

    /**
     * The transformation from target domain to source domain
     */
    abstract Affine inverse();

    /**
     * invert this transformation's domain.
     *
     * @return a transformation.
     */
    public abstract ProbeTransform<C2, C1> inverted();

    /**
     * {@return source domain}
     */
    public Domain<C1> sourceDomain() {
        return source;
    }

    /**
     * {@return target domain}
     */
    public Domain<C2> targetDomain() {
        return target;
    }

    /**
     * {@return a copied transform}
     */
    public Affine getTransform() {
        return new Affine(transform());
    }

    /**
     * Compose transformation.
     *
     * @param domain    target domain
     * @param transform Composed transformation
     * @param <C3>      coordinate from target domain.
     * @return a transformation from {@code Domain<C1>} to {@code Domain <C3>}
     * @see #then(ProbeTransform)
     */
    public abstract <C3> ProbeTransform<C1, C3> then(Domain<C3> domain, Affine transform);

    /**
     * compose {@code transform}. It returns a transformation {@code transform(this(.))} ({@code transform ∘ this}).
     *
     * @param transform Composed transformation
     * @param <C3>      coordinate from target domain.
     * @return a transformation from {@code Domain<C1>} to {@code Domain <C3>}
     * @see #then(Domain, Affine)
     */
    public abstract <C3> ProbeTransform<C1, C3> then(ProbeTransform<C2, C3> transform);

    /**
     * Check the {@code transform} can be composed with this.
     *
     * @param transform transformation
     */
    void checkComposeDomain(ProbeTransform<?, ?> transform) {
        if (!targetDomain().equals(transform.sourceDomain())) {
            throw new RuntimeException("domain mismatch between " + targetDomain() + " and " + transform.sourceDomain());
        }
    }

    /*======================*
     * point transformation *
     *======================*/

    /**
     * transform a point from source domain.
     *
     * @param x x position in source domain.
     * @param y y position in source domain.
     * @param z z position in source domain.
     * @return point in target domain.
     */
    public Point3D transform(double x, double y, double z) {
        return transform().transform(x, y, z);
    }

    /**
     * transform a point from source domain.
     *
     * @param p point in source domain.
     * @return point in target domain.
     */
    public Point3D transform(Point3D p) {
        return transform().transform(p);
    }

    /**
     * transform a coordinate from source domain.
     *
     * @param coordinate coordinate in source domain.
     * @return coordinate in target domain.
     */
    public C2 transform(C1 coordinate) {
        return target.fromPoint(transform().transform(source.toPoint(coordinate)));
    }

    /**
     * transform a point from target domain.
     *
     * @param x x position in target domain.
     * @param y y position in target domain.
     * @param z z position in target domain.
     * @return point in source domain.
     */
    public Point3D inverseTransform(double x, double y, double z) {
        return inverse().transform(x, y, z);
    }

    /**
     * transform a point from target domain.
     *
     * @param p point in target domain.
     * @return point in source domain.
     */
    public Point3D inverseTransform(Point3D p) {
        return inverse().transform(p);
    }

    /**
     * transform a coordinate from target domain.
     *
     * @param coordinate coordinate in target domain.
     * @return coordinate in source domain.
     */
    public C1 inverseTransform(C2 coordinate) {
        return source.fromPoint(inverse().transform(target.toPoint(coordinate)));
    }


    /**
     * transform a delta from source domain.
     *
     * @param x x delta in source domain.
     * @param y y delta in source domain.
     * @param z z delta in source domain.
     * @return delta in target domain.
     */
    public Point3D deltaTransform(double x, double y, double z) {
        return transform().deltaTransform(x, y, z);
    }

    /**
     * transform a delta from source domain.
     *
     * @param p delta in source domain.
     * @return delta in target domain.
     */
    public Point3D deltaTransform(Point3D p) {
        return transform().deltaTransform(p);
    }

    /**
     * transform a delta from target domain.
     *
     * @param x x delta in target domain.
     * @param y y delta in target domain.
     * @param z z delta in target domain.
     * @return delta in source domain.
     */
    public Point3D inverseDeltaTransform(double x, double y, double z) {
        return inverse().deltaTransform(x, y, z);
    }

    /**
     * transform a delta from target domain.
     *
     * @param p delta in target domain.
     * @return delta in source domain.
     */
    public Point3D inverseDeltaTransform(Point3D p) {
        return inverse().deltaTransform(p);
    }

    /*===========*
     * factories *
     *===========*/

    /**
     * Create an identify transformation.
     *
     * @param domain domain.
     * @param <C>    coordinate
     * @return a transformation
     */
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
     * @return a transformation
     */
    public static ProbeTransform<ProbeCoordinate, Coordinate> create(Affine transform) {
        return new FixedTransform<>(PROBE, ANATOMICAL, transform);
    }

    /**
     * Create a coordinate transformation from {@link Coordinate} to a referenced {@link Coordinate}.
     * <p>
     * The transform matrix {@snippet lang = "TEXT":
     * [  1  0  0 -ap ]
     * [  0  1  0 -dv ]
     * [  0  0  1 -ml ]
     *}
     * and the transform matrix for {@code flipAP} {@snippet lang = "TEXT":
     * [ -1  0  0  ap ]
     * [  0  1  0 -dv ]
     * [  0  0 -1  ml ]
     *}
     *
     * @param reference the name of the reference.
     * @param origin    the origin of the reference in the global anatomical space.
     * @param flipAP    flip the AP-axis
     * @return a transformation
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
     * Create a coordinate transform based on the {@code implant}.
     *
     * @param implant an implant coordinate
     * @return a transformation
     * @throws IllegalArgumentException {@code implant} does not reference to global anatomical space,
     *                                  which means  {@link ImplantCoordinate#reference} is not {@code null}.
     */
    public static ProbeTransform<ProbeCoordinate, Coordinate> create(ImplantCoordinate implant) {
        if (implant.reference() != null) throw new IllegalArgumentException("not reference to global");
        var a = new Point3D(implant.ap(), implant.dv(), implant.ml());
        var t = new Affine();
        t.appendTranslation(implant.ap(), implant.dv() + implant.depth(), implant.ml());
        t.appendRotation(implant.rap(), a, AXIS_AP);
        t.appendRotation(implant.rdv(), a, AXIS_DV);
        t.appendRotation(implant.rml(), a, AXIS_ML);
        return create(t);
    }

    /**
     * Create transformation from {@code source} to {@code target} with given fixed {@code transform}.
     *
     * @param source    source domain
     * @param target    target domain
     * @param transform a transform
     * @param <C1>      coordinate in source domain
     * @param <C2>      coordinate in target domain
     * @return a transformation
     */
    public static <C1, C2> ProbeTransform<C1, C2> create(Domain<C1> source, Domain<C2> target, Affine transform) {
        return new FixedTransform<>(source, target, transform);
    }

    /**
     * Create transformation from {@code source} to {@code target} with given dynamic {@code transform}.
     *
     * @param source    source domain
     * @param target    target domain
     * @param transform a transform supplier.
     * @param <C1>      coordinate in source domain
     * @param <C2>      coordinate in target domain
     * @return a transformation
     */
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
            this.transform = new Affine(transform);

            try {
                this.inverse = transform.createInverse();
            } catch (NonInvertibleTransformException e) {
                throw new RuntimeException(e);
            }
        }

        private FixedTransform(Domain<C1> d1, Domain<C2> d2, Affine transform, Affine inverse) {
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
            return new FixedTransform<>(target, source, inverse, transform);
        }

        public <C3> ProbeTransform<C1, C3> then(Domain<C3> domain, Affine transform) {
            var t = new Affine(transform);
            t.prepend(transform);
            return new FixedTransform<>(this.source, domain, t);
        }

        public <C3> ProbeTransform<C1, C3> then(ProbeTransform<C2, C3> transform) {
            checkComposeDomain(transform);
            if (transform instanceof ProbeTransform.FixedTransform<C2, C3> f) {
                var t = new Affine(this.transform);
                t.prepend(f.transform());
                return new FixedTransform<>(this.source, transform.target, t);
            } else if (transform instanceof ProbeTransform.ComposedTransform<C2, ?, C3> f) {
                return then(f);
            } else {
                return new ComposedTransform<>(this, transform);
            }
        }

        public <C3, C4> ProbeTransform<C1, C4> then(ComposedTransform<C2, C3, C4> transform) {
            if (transform.f instanceof ProbeTransform.FixedTransform<C2, C3> f) {
                return new ComposedTransform<>(then(f), transform.g);
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
            return new DynamicTransform<>(target, source, transform, !inverted);
        }

        @Override
        public <C3> ProbeTransform<C1, C3> then(Domain<C3> domain, Affine transform) {
            return new ComposedTransform<>(this, new FixedTransform<>(targetDomain(), domain, transform));
        }

        @Override
        public <C3> ProbeTransform<C1, C3> then(ProbeTransform<C2, C3> transform) {
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
            // (T1, (T2 ...(Tn)))
            // [Tn]...[T2][T1]
            var t = new Affine(f.transform());
            t.prepend(g.transform());
            return t;
        }

        @Override
        protected Affine inverse() {
            // (T1, (T2 ...(Tn)))
            // [T1'][T2']...[Tn']
            var t = new Affine(g.inverse());
            t.prepend(f.inverse());
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
        | (S, C) -> then(inverted(C) , S')
        e.g. inverted((S1, (S2, S3))) = (S3', (S2', S1'))
        e.g. inverted((S1, (S2, (... (Sn))))) = (Sn', (..., (S2', S1')))

        then(T, T)
        | (F, F) -> F
        | (F, (F, C)) -> (then(F, F), C)
        | (S, T) -> (S, T)
        | ((S, T1), T2) -> (S, then(T1, T2))
        e.g. then((S1, (S2, S3)), S4) = (S1, (S2, (S3, S4)))
         */

        @Override
        public ProbeTransform<C3, C1> inverted() {
            return inverted(f, g);
        }

        private static <C1, C2, C3> ProbeTransform<C3, C1> inverted(ProbeTransform<C1, C2> f, ProbeTransform<C2, C3> g) {
            assert !(f instanceof ProbeTransform.ComposedTransform<C1, ?, C2>);
            if (g instanceof ProbeTransform.ComposedTransform<C2, ?, C3> gg) {
                return thenInvert(gg, f.inverted());
            } else {
                return new ComposedTransform<>(g.inverted(), f.inverted());
            }
        }

        private static <C1, C2, C3, C4> ProbeTransform<C4, C1> thenInvert(ComposedTransform<C2, C3, C4> c, ProbeTransform<C2, C1> r) {
            var rr = new ComposedTransform<>(c.f.inverted(), r);

            if (c.g instanceof ProbeTransform.ComposedTransform<C3, ?, C4> cg) {
                return thenInvert(cg, rr);
            } else {
                return new ComposedTransform<>(c.g.inverted(), rr);
            }
        }

        @Override
        public <C4> ProbeTransform<C1, C4> then(Domain<C4> domain, Affine transform) {
            return then(new FixedTransform<>(targetDomain(), domain, transform));
        }

        @Override
        public <C4> ProbeTransform<C1, C4> then(ProbeTransform<C3, C4> transform) {
            checkComposeDomain(transform);
            return new ComposedTransform<>(f, g.then(transform));
        }
    }
}
