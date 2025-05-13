package io.ast.jneurocarto.atlas;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.CoordinateIndex;
import io.ast.jneurocarto.core.ImplantCoordinate;

@NullMarked
public final class ImageSliceStack {

    public enum Projection {
        coronal(/*AP*/0, /*ML*/2, /*DV*/1), sagittal(/*ML*/2, /*AP*/0, /*DV*/1), transverse(/*DV*/1, /*ML*/2, /*AP*/0);

        public final int p;
        public final int x;
        public final int y;

        Projection(int p, int x, int y) {
            this.p = p;
            this.x = x;
            this.y = y;
        }

        int get(int i) {
            return switch (i) {
                case 0 -> p;
                case 1 -> x;
                case 2 -> y;
                default -> throw new IllegalArgumentException();
            };
        }

        public int get(CoordinateIndex coor, int i) {
            return switch (i) {
                case 0 -> coor.ap();
                case 1 -> coor.dv();
                case 2 -> coor.ml();
                default -> throw new IllegalArgumentException();
            };
        }

        public double get(Coordinate coor, int i) {
            return switch (i) {
                case 0 -> coor.ap();
                case 1 -> coor.dv();
                case 2 -> coor.ml();
                default -> throw new IllegalArgumentException();
            };
        }
    }

    private final double[] brainResolution;
    private final int[] volumeShape;
    private final @Nullable ImageVolume volume;
    private final Projection project;
    private final double[] resolution; // {p, x, y}

    public ImageSliceStack(BrainAtlas brain, ImageVolume volume, Projection project) {
        brainResolution = brain.resolution();
        this.volume = volume;
        volumeShape = volume.shape();
        this.project = project;

        this.resolution = new double[3];

        this.resolution[0] = brainResolution[project.p];
        this.resolution[1] = brainResolution[project.x];
        this.resolution[2] = brainResolution[project.y];
    }

    /**
     * Test-purpose constructor.
     *
     * @param brainResolution
     * @param volumeShape
     * @param project
     */
    ImageSliceStack(double brainResolution, int[] volumeShape, Projection project) {
        this(new double[]{brainResolution, brainResolution, brainResolution}, volumeShape, project);
    }

    /**
     * Test-purpose constructor.
     *
     * @param brainResolution
     * @param volumeShape
     * @param project
     */
    ImageSliceStack(double[] brainResolution, int[] volumeShape, Projection project) {
        this.brainResolution = brainResolution;
        this.volumeShape = volumeShape;
        this.project = project;
        volume = null;

        this.resolution = new double[3];

        this.resolution[0] = brainResolution[project.p];
        this.resolution[1] = brainResolution[project.x];
        this.resolution[2] = brainResolution[project.y];
    }

    public Projection projection() {
        return project;
    }

    public ImageVolume getVolume() {
        return Objects.requireNonNull(volume);
    }

    public int plane() {
        return dimensionOnAxes(0);
    }

    public int width() {
        return dimensionOnAxes(1);
    }

    public int height() {
        return dimensionOnAxes(2);
    }

    /**
     * {@return resolution double array on (p, x, y) axis}
     */
    public double[] resolution() {
        return resolution;
    }

    public double planeUm() {
        return lengthOnAxes(0);
    }

    public double widthUm() {
        return lengthOnAxes(1);
    }

    public double heightUm() {
        return lengthOnAxes(2);
    }

    /**
     * @param axis {0: page, 1: width (x), 2: height (y)}
     * @return
     */
    public int dimensionOnAxes(int axis) {
        return volumeShape[project.get(axis)];
    }

    /**
     * @param axis {0: page, 1: width (x), 2: height (y)}
     * @return um
     */
    public double lengthOnAxes(int axis) {
        var i = project.get(axis);
        return volumeShape[i] * brainResolution[i];
    }

    public SliceCoordinate project(Coordinate coor) {
        return project(coor.toCoorIndex(brainResolution)).toCoor(resolution);
    }

    /**
     * project coordinate (AP, DV, ML) into (p, x, y)
     *
     * @param coor coordinate (AP, DV, ML)
     * @return coordinate (p, x, y)
     */
    public SliceCoordinateIndex project(CoordinateIndex coor) {
        return new SliceCoordinateIndex(
          project.get(coor, project.p),
          project.get(coor, project.x),
          project.get(coor, project.y)
        );
    }

    public Coordinate pullBack(SliceCoordinate coor) {
        return pullBack(coor.toCoorIndex(resolution)).toCoor(brainResolution);
    }

    /**
     * project coordinate (p, x, y) into (AP, DV, ML)
     *
     * @param coor coordinate (p, x, y)
     * @return coordinate (AP, DV, ML)
     */
    public CoordinateIndex pullBack(SliceCoordinateIndex coor) {
        var t = new int[3];

        t[project.p] = coor.p();
        t[project.x] = coor.x();
        t[project.y] = coor.y();

        return new CoordinateIndex(t[0], t[1], t[2]);
    }

    /**
     * @param rotate rotation on (ap, dv, ml). Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     * @return offset on (x, y). Reuse {@link SliceCoordinate} but changing fields' meaning to offset. {@link SliceCoordinate#p()} is not used.
     */
    public SliceCoordinate angle2Offset(Coordinate rotate) {
        var rx = project.get(rotate, project.x);
        var ry = project.get(rotate, project.y);

        var dw = -lengthOnAxes(project.x) * Math.tan(ry) / 2;
        var dh = lengthOnAxes(project.y) * Math.tan(rx) / 2;
        return new SliceCoordinate(0, dw, dh);
    }

    /**
     * @param dw offset on width-side edge
     * @param dh offset on height-side edge
     * @return rotation on (ap, dv, ml). Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     */
    public Coordinate offset2Angle(int dw, int dh) {
        var ry = Math.atan(-(double) dw * 2 / dimensionOnAxes(project.x));
        var rx = Math.atan((double) dh * 2 / dimensionOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new Coordinate(t[0], t[1], t[2]);
    }

    /**
     * @param dw offset (um) on width-side edge
     * @param dh offset (um) on height-side edge
     * @return rotation on (ap, dv, ml). Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     */
    public Coordinate offset2Angle(double dw, double dh) {
        var ry = Math.atan(-dw * 2 / lengthOnAxes(project.x));
        var rx = Math.atan(dh * 2 / lengthOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new Coordinate(t[0], t[1], t[2]);
    }

    /**
     * @param offset offset. Reuse {@link SliceCoordinateIndex} but changing fields' meaning to offset. {@link SliceCoordinateIndex#p()} is not used.
     * @return rotation on (ap, dv, ml). Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     */
    public Coordinate offset2Angle(SliceCoordinateIndex offset) {
        var ry = Math.atan(-(double) offset.x() * 2 / dimensionOnAxes(project.x));
        var rx = Math.atan((double) offset.y() * 2 / dimensionOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new Coordinate(t[0], t[1], t[2]);
    }

    /**
     * @param offset offset (um). Reuse {@link SliceCoordinate} but changing fields' meaning to offset. {@link SliceCoordinate#p()} is not used.
     * @return rotation on (ap, dv, ml). Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     */
    public Coordinate offset2Angle(SliceCoordinate offset) {
        var ry = Math.atan(-offset.x() * 2 / lengthOnAxes(project.x));
        var rx = Math.atan(offset.y() * 2 / lengthOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new Coordinate(t[0], t[1], t[2]);
    }

    public ImageSlice sliceAtPlane(int plane) {
        int ax = width() / 2;
        int ay = height() / 2;
        return new ImageSlice(plane, ax, ay, 0, 0, this);
    }

    public ImageSlice sliceAtPlane(double plane) {
        var resolution = brainResolution[project.p];
        return sliceAtPlane((int) (plane / resolution));
    }

    public ImageSlice sliceAtPlane(SliceCoordinateIndex coor) {
        return new ImageSlice(coor.p(), coor.x(), coor.y(), 0, 0, this);
    }

    public ImageSlice sliceAtPlane(SliceCoordinate coor) {
        return sliceAtPlane(coor.toCoorIndex(resolution));
    }

    public ImageSlice sliceAtPlane(CoordinateIndex coor) {
        return sliceAtPlane(project(coor));
    }

    public ImageSlice sliceAtPlane(Coordinate coor) {
        return sliceAtPlane(project(coor.toCoorIndex(brainResolution)));
    }

    public ImageSlice sliceAtPlane(ImplantCoordinate coor) {
        coor = coor.toOrigin();
        var rot = coor.rotation();
        var offset = angle2Offset(rot);
        return sliceAtPlane(coor).withOffset(offset);
    }

    public ImageSlice sliceAtPlane(ImageSlice slice) {
        if (slice.projection() != project) throw new IllegalArgumentException("different projection");
        return new ImageSlice(slice.plane(), slice.ax(), slice.ay(), slice.dw(), slice.dh(), this);
    }
}
