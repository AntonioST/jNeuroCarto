package io.ast.jneurocarto.atlas;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ImageSlices {

    public enum View {
        coronal(/*AP*/0, /*ML*/2, /*DV*/1), sagittal(/*ML*/2, /*AP*/0, /*DV*/1), transverse(/*DV*/1, /*ML*/2, /*AP*/0);

        public final int p;
        public final int x;
        public final int y;

        View(int p, int x, int y) {
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

        public int get(BrainAtlas.CoordinateIndex coor, int i) {
            return switch (i) {
                case 0 -> coor.ap();
                case 1 -> coor.dv();
                case 2 -> coor.ml();
                default -> throw new IllegalArgumentException();
            };
        }

        public double get(BrainAtlas.Coordinate coor, int i) {
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
    private final View project;
    private final double[] resolution; // {p, x, y}

    public ImageSlices(BrainAtlas brain, ImageVolume volume, View project) {
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
    ImageSlices(double brainResolution, int[] volumeShape, View project) {
        this(new double[]{brainResolution, brainResolution, brainResolution}, volumeShape, project);
    }

    /**
     * Test-purpose constructor.
     *
     * @param brainResolution
     * @param volumeShape
     * @param project
     */
    ImageSlices(double[] brainResolution, int[] volumeShape, View project) {
        this.brainResolution = brainResolution;
        this.volumeShape = volumeShape;
        this.project = project;
        volume = null;

        this.resolution = new double[3];

        this.resolution[0] = brainResolution[project.p];
        this.resolution[1] = brainResolution[project.x];
        this.resolution[2] = brainResolution[project.y];
    }

    public View view() {
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

    /**
     * coordinate system in image volume space.
     *
     * @param p um
     * @param x um
     * @param y um
     */
    public record Coordinate(double p, double x, double y) {
        public CoordinateIndex toCoorIndex(double resolution) {
            return new CoordinateIndex((int) (p / resolution), (int) (x / resolution), (int) (y / resolution));
        }

        /**
         * @param resolution int array of {p, x, y}
         * @return
         */
        public CoordinateIndex toCoorIndex(double[] resolution) {
            if (resolution.length != 3) throw new IllegalArgumentException();
            return new CoordinateIndex((int) (p / resolution[0]), (int) (x / resolution[1]), (int) (y / resolution[2]));
        }
    }

    /**
     * coordinate system in image volume space.
     *
     * @param p
     * @param x
     * @param y
     */
    public record CoordinateIndex(int p, int x, int y) {
        public Coordinate toCoor(double resolution) {
            return new Coordinate(p * resolution, x * resolution, y * resolution);
        }

        /**
         * @param resolution int array of {p, x, y}
         * @return
         */
        public Coordinate toCoor(double[] resolution) {
            if (resolution.length != 3) throw new IllegalArgumentException();
            return new Coordinate(p * resolution[0], x * resolution[1], y * resolution[2]);
        }
    }

    public Coordinate project(BrainAtlas.Coordinate coor) {
        return project(coor.toCoorIndex(brainResolution)).toCoor(resolution);
    }

    /**
     * project coordinate (AP, DV, ML) into (p, x, y)
     *
     * @param coor coordinate (AP, DV, ML)
     * @return coordinate (p, x, y)
     */
    public CoordinateIndex project(BrainAtlas.CoordinateIndex coor) {
        return new CoordinateIndex(
          project.get(coor, project.p),
          project.get(coor, project.x),
          project.get(coor, project.y)
        );
    }

    public BrainAtlas.Coordinate pullBack(Coordinate coor) {
        return pullBack(coor.toCoorIndex(resolution)).toCoor(brainResolution);
    }

    /**
     * project coordinate (p, x, y) into (AP, DV, ML)
     *
     * @param coor coordinate (p, x, y)
     * @return coordinate (AP, DV, ML)
     */
    public BrainAtlas.CoordinateIndex pullBack(CoordinateIndex coor) {
        var t = new int[3];

        t[project.p] = coor.p();
        t[project.x] = coor.x();
        t[project.y] = coor.y();

        return new BrainAtlas.CoordinateIndex(t[0], t[1], t[2]);
    }

    /**
     * @param rotate rotation on (ap, dv, ml). Reuse {@link BrainAtlas.Coordinate} but changing fields' meaning to roration radians.
     * @return offset on (x, y). Reuse {@link Coordinate} but changing fields' meaning to offset. {@link Coordinate#p} is not used.
     */
    public Coordinate angle2Offset(BrainAtlas.Coordinate rotate) {
        var rx = project.get(rotate, project.x);
        var ry = project.get(rotate, project.y);

        var dw = -lengthOnAxes(project.x) * Math.tan(ry) / 2;
        var dh = lengthOnAxes(project.y) * Math.tan(rx) / 2;
        return new Coordinate(0, dw, dh);
    }

    /**
     * @param dw offset on width-side edge
     * @param dh offset on height-side edge
     * @return rotation on (ap, dv, ml). Reuse {@link BrainAtlas.Coordinate} but changing fields' meaning to roration radians.
     */
    public BrainAtlas.Coordinate offset2Angle(int dw, int dh) {
        var ry = Math.atan(-(double) dw * 2 / dimensionOnAxes(project.x));
        var rx = Math.atan((double) dh * 2 / dimensionOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new BrainAtlas.Coordinate(t[0], t[1], t[2]);
    }

    /**
     * @param dw offset (um) on width-side edge
     * @param dh offset (um) on height-side edge
     * @return rotation on (ap, dv, ml). Reuse {@link BrainAtlas.Coordinate} but changing fields' meaning to roration radians.
     */
    public BrainAtlas.Coordinate offset2Angle(double dw, double dh) {
        var ry = Math.atan(-dw * 2 / lengthOnAxes(project.x));
        var rx = Math.atan(dh * 2 / lengthOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new BrainAtlas.Coordinate(t[0], t[1], t[2]);
    }

    /**
     * @param offset offset. Reuse {@link CoordinateIndex} but changing fields' meaning to offset. {@link CoordinateIndex#p} is not used.
     * @return rotation on (ap, dv, ml). Reuse {@link BrainAtlas.Coordinate} but changing fields' meaning to roration radians.
     */
    public BrainAtlas.Coordinate offset2Angle(CoordinateIndex offset) {
        var ry = Math.atan(-(double) offset.x() * 2 / dimensionOnAxes(project.x));
        var rx = Math.atan((double) offset.y() * 2 / dimensionOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new BrainAtlas.Coordinate(t[0], t[1], t[2]);
    }

    /**
     * @param offset offset (um). Reuse {@link Coordinate} but changing fields' meaning to offset. {@link Coordinate#p} is not used.
     * @return rotation on (ap, dv, ml). Reuse {@link BrainAtlas.Coordinate} but changing fields' meaning to roration radians.
     */
    public BrainAtlas.Coordinate offset2Angle(Coordinate offset) {
        var ry = Math.atan(-offset.x() * 2 / lengthOnAxes(project.x));
        var rx = Math.atan(offset.y() * 2 / lengthOnAxes(project.y));

        var t = new double[3];
        t[project.p] = 0;
        t[project.x] = rx;
        t[project.y] = ry;

        return new BrainAtlas.Coordinate(t[0], t[1], t[2]);
    }

    public ImageSlice sliceAtPlace(int plane) {
        int ax = width() / 2;
        int ay = height() / 2;
        return new ImageSlice(plane, ax, ay, 0, 0, this);
    }

    public ImageSlice sliceAtPlace(double plane) {
        var resolution = brainResolution[project.p];
        return sliceAtPlace((int) (plane / resolution));
    }

    public ImageSlice sliceAtPlace(CoordinateIndex coor) {
        return new ImageSlice(coor.p, coor.x, coor.y, 0, 0, this);
    }

    public ImageSlice sliceAtPlace(Coordinate coor) {
        return sliceAtPlace(coor.toCoorIndex(resolution));
    }

    public ImageSlice sliceAtPlace(BrainAtlas.CoordinateIndex coor) {
        return sliceAtPlace(project(coor));
    }

    public ImageSlice sliceAtPlace(BrainAtlas.Coordinate coor) {
        return sliceAtPlace(project(coor.toCoorIndex(brainResolution)));
    }
}
