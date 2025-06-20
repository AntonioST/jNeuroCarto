package io.ast.jneurocarto.atlas;

import java.awt.image.BufferedImage;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.CoordinateIndex;
import io.ast.jneurocarto.core.ProbeTransform;
import io.ast.jneurocarto.core.numpy.FlatIntArray;

/**
 * @param plane
 * @param ax    anchor x position
 * @param ay    anchor y position
 * @param dw    offset at width-edge
 * @param dh    offset at width-edge
 * @param stack
 */
@NullMarked
public record ImageSlice(int plane, int ax, int ay, int dw, int dh, ImageSliceStack stack) {

    public ImageSliceStack.Projection projection() {
        return stack.projection();
    }

    /**
     * {@return resolution double array on (p, x, y) axis}
     */
    public double[] resolution() {
        return stack.resolution();
    }

    /**
     * {@return plane in um}
     */
    public double planeLength() {
        return plane * resolution()[0];
    }

    /**
     * {@return width in um.}
     */
    public double width() {
        return stack.widthUm();
    }

    /**
     * {@return height in um}
     */
    public double height() {
        return stack.heightUm();
    }

    /**
     * {@return width in pixels}
     */
    public int widthPx() {
        return stack.width();
    }

    /**
     * {@return height in pixels}
     */
    public int heightPx() {
        return stack.height();
    }

    public int planeAt(int x, int y) {
        if (dw == 0 && dh == 0) return plane;

        var cx = width() / 2;
        var cy = height() / 2;
        var dw = this.dw * resolution()[1] / cx * (x * resolution()[1] - cx);
        var dh = this.dh * resolution()[2] / cy * (y * resolution()[2] - cy);
        var dp = plane + dw + dh;
        return (int) (dp / resolution()[0]);
    }

    public double planeAt(double x, double y) {
        if (dw == 0 && dh == 0) return plane * resolution()[0];

        var cx = width() / 2;
        var cy = height() / 2;
        var dw = this.dw * resolution()[1] / cx * (x - cx);
        var dh = this.dh * resolution()[2] / cy * (y - cy);
        return plane * resolution()[0] + dw + dh;
    }

    /**
     * @param coor
     * @return return {@code coor} but updating {@link SliceCoordinateIndex#p()} as result.
     */
    public SliceCoordinateIndex planeAt(SliceCoordinateIndex coor) {
        var p = planeAt(coor.x(), coor.y());
        return new SliceCoordinateIndex(p, coor.x(), coor.y());
    }

    /**
     * @param coor
     * @return return {@code coor} but updating {@link SliceCoordinate#p()} as result.
     */
    public SliceCoordinate planeAt(SliceCoordinate coor) {
        var p = planeAt(coor.x(), coor.y());
        return new SliceCoordinate(p, coor.x(), coor.y());
    }

    public SliceCoordinate planeAt(Point2D coor) {
        var p = planeAt(coor.getX(), coor.getY());
        return new SliceCoordinate(p, coor);
    }

    public Affine planeAtTransform() {
        var p = plane * resolution()[0];
        var cx = width() / 2;
        var cy = height() / 2;
        var dw = this.dw * resolution()[1] / cx;
        var dh = this.dh * resolution()[2] / cy;

        return new Affine(
            /*x*/ 1, 0, 0, 0,
            /*y*/ 0, 1, 0, 0,
            /*p*/ dw, dh, 0, p - dw * cx - dh * cy
        );
    }

    /**
     * {@return a transformation from slice space to slice space with plane filled}
     */
    public ProbeTransform<SliceCoordinate, SliceCoordinate> getPlaneAtTransform() {
        return ProbeTransform.create(SliceDomain.INSTANCE, SliceDomain.INSTANCE, planeAtTransform());
    }

    /**
     * project coordinate (AP, DV, ML) into (p, x, y)
     *
     * @param coor coordinate (AP, DV, ML)
     * @return coordinate (p, x, y),
     * where {@link SliceCoordinateIndex#p()} will be replaced by {@link #planeAt(SliceCoordinateIndex)}.
     */
    public SliceCoordinate project(Coordinate coor) {
        return planeAt(stack.project(coor));
    }

    /**
     * project coordinate (AP, DV, ML) into (p, x, y)
     *
     * @param coor coordinate (AP, DV, ML)
     * @return coordinate (p, x, y),
     * where {@link SliceCoordinateIndex#p()} will be replaced by {@link #planeAt(SliceCoordinateIndex)}.
     */
    public SliceCoordinateIndex project(CoordinateIndex coor) {
        return planeAt(stack.project(coor));
    }

    /**
     * project coordinate (x, y) into (AP, DV, ML)
     *
     * @param x slice coordinate (um).
     * @param y slice coordinate (um).
     * @return global anatomical coordinate
     */
    public Coordinate pullBack(double x, double y) {
        return stack.pullBack(new SliceCoordinate(planeAt(x, y), x, y));
    }

    /**
     * project coordinate (x, y) into (AP, DV, ML)
     *
     * @param coor coordinate (p, x, y),
     *             where {@link SliceCoordinate#p()} is ignored and will be replaced by {@link #planeAt(SliceCoordinate)}.
     * @return global anatomical coordinate
     */
    public Coordinate pullBack(SliceCoordinate coor) {
        return stack.pullBack(planeAt(coor));
    }

    /**
     * project coordinate (x, y) into (AP, DV, ML)
     *
     * @param coor coordinate (p, x, y),
     *             where {@link SliceCoordinateIndex#p()} will be replaced by {@link #planeAt(SliceCoordinateIndex)}.
     * @return coordinate (AP, DV, ML)
     */
    public CoordinateIndex pullBack(SliceCoordinateIndex coor) {
        return stack.pullBack(planeAt(coor));
    }

    public Coordinate pullBack(Point2D p) {
        return stack.pullBack(planeAt(p));
    }

    /**
     * {@return a transformation from slice space to global anatomical space}.
     */
    public ProbeTransform<SliceCoordinate, Coordinate> getTransform() {
        var t = stack.getTransform();
        return getPlaneAtTransform().then(t);
    }

    /**
     * get rotation on (ap, dv, ml).
     *
     * @return rotation on (ap, dv, ml). Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     */
    public Coordinate offset2Angle() {
        return stack.offset2Angle(dw, dh);
    }

    public ImageSlice withPlane(int plane) {
        return new ImageSlice(plane, ax, ay, dw, dh, stack);
    }

    public ImageSlice withAnchor(int ax, int ay) {
        var plane = planeAt(ax, ay);
        return new ImageSlice(plane, ax, ay, dw, dh, stack);
    }

    public ImageSlice withAnchor(double ax, double ay) {
        var resolution = resolution();
        return withAnchor((int) (ax / resolution[1]), (int) (ay / resolution[2]));
    }

    public ImageSlice withAnchor(SliceCoordinateIndex coor) {
        return withAnchor(coor.x(), coor.y());
    }

    public ImageSlice withAnchor(SliceCoordinate coor) {
        return withAnchor(coor.x(), coor.y());
    }

    public ImageSlice withAnchor(Point2D coor) {
        return withAnchor(coor.getX(), coor.getY());
    }

    public ImageSlice withOffset(int dw, int dh) {
        return new ImageSlice(plane, ax, ay, dw, dh, stack);
    }

    public ImageSlice withOffset(double dw, double dh) {
        var res = stack.resolution()[0];
        var iw = (int) (dw / res);
        var ih = (int) (dh / res);
        return new ImageSlice(plane, ax, ay, iw, ih, stack);
    }

    /**
     * @param offset offset on (x, y). Reuse {@link SliceCoordinate} but changing fields' meaning to offset. {@link SliceCoordinate#p()} is not used.
     * @return
     */
    public ImageSlice withOffset(SliceCoordinate offset) {
        return withOffset(offset.x(), offset.y());
    }

    /**
     * @param offset offset on (x, y). Reuse {@link SliceCoordinateIndex} but changing fields' meaning to offset. {@link SliceCoordinateIndex#p()} is not used.
     * @return
     */
    public ImageSlice withOffset(SliceCoordinateIndex offset) {
        return withOffset(offset.x(), offset.y());
    }

    public ImageSlice withRotate(int rx, int ry) {
        var dw = -width() * Math.tan(ry) / 2;
        var dh = height() * Math.tan(rx) / 2;
        var res = resolution()[0];
        return new ImageSlice(plane, ax, ay, (int) (dw / res), (int) (dh / res), stack);
    }

    /*===============*
     * image writing *
     *===============*/

    public static final ImageWriter<FlatIntArray> INT_IMAGE = new ArrayImageWriter();
    public static final ImageWriter<BufferedImage> AWT_IMAGE = new BufferedImageWriter();
    public static final ImageWriter<Image> JFX_IMAGE = new JavaFxImageWriter();

    public interface ImageWriter<T> {
        void create(int w, int h, @Nullable T init);

        void set(int x, int y, int v);

        T get();
    }

    public static final class ArrayImageWriter implements ImageWriter<FlatIntArray> {
        private FlatIntArray image;
        private int w;
        private int h;

        @Override
        public void create(int w, int h, @Nullable FlatIntArray init) {
            this.w = w;
            this.h = h;
            if (init != null && !checkShape(init.shape())) {
                image = init;
            } else {
                image = new FlatIntArray(new int[]{h, w}, new int[w * h]);
            }
        }

        private boolean checkShape(int[] shape) {
            return shape.length == 2 && shape[0] == h && shape[1] == w;
        }

        @Override
        public void set(int x, int y, int v) {
            image.array()[y * w + x] = v;
        }

        @Override
        public FlatIntArray get() {
            return image;
        }
    }

    private static final class BufferedImageWriter implements ImageWriter<BufferedImage> {
        private BufferedImage image;

        @Override
        public void create(int w, int h, @Nullable BufferedImage init) {
            image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        }

        @Override
        public void set(int x, int y, int v) {
            image.setRGB(x, y, v);
        }

        @Override
        public BufferedImage get() {
            return image;
        }
    }

    private static final class JavaFxImageWriter implements ImageWriter<Image> {
        private WritableImage image;
        private PixelWriter writer;

        @Override
        public void create(int w, int h, @Nullable Image init) {
            image = new WritableImage(w, h);
            writer = image.getPixelWriter();
        }

        @Override
        public void set(int x, int y, int v) {
            writer.setArgb(x, y, v);
        }

        @Override
        public Image get() {
            return image;
        }
    }

    public <T> T image(ImageWriter<T> writer) {
        return image(writer, null);
    }

    public <T> T image(ImageWriter<T> writer, @Nullable T init) {
        var w = stack.width();
        var h = stack.height();
        return image(writer, 0, 0, w, h, init);
    }

    public <T> T image(ImageWriter<T> writer, int x, int y, int w, int h, @Nullable T init) {
        var resolution = resolution();
        var rp = resolution[0];
        var rx = resolution[1];
        var ry = resolution[2];

        var plane = this.plane;
        var width = stack.width();
        var height = stack.height();
        if (x < 0 || y < 0 || w > width || h > height || x + w > width || y + h > height) {
            throw new IllegalArgumentException();
        }

        var cx = width * rx / 2;
        var cy = height * ry / 2;

        var volume = stack.getVolume();
        writer.create(w, h, init);

        var dw = new int[width];
        var dh = new int[height];

        var fw = this.dw * rx / cx;
        var fh = this.dh * ry / cy;
        for (int xx = 0; xx < width; xx++) {
            dw[xx] = (int) (fw * (xx * rx - cx) / rp);
        }
        for (int yy = 0; yy < height; yy++) {
            dh[yy] = (int) (fh * (yy * ry - cy) / rp);
        }
        var dp = dw[ax] + dh[ay];

        var px = stack.plane();

        var project = projection();
        var q = new int[3];

        q[project.p] = 0;
        q[project.x] = 1;
        q[project.y] = 0;

        var dx = volume.index(q[0], q[2], q[1]);

        q[project.p] = 0;
        q[project.x] = 0;
        q[project.y] = 1;

        var dy = volume.index(q[0], q[2], q[1]);

        /*
        coronal     dx = 456,   dy = 1
        transverse  dx = 456,   dy = 145920
        sagittal    dx = 145920, dy = 1
         */

        if (dx < dy) { // transverse
            for (int y0 = 0; y0 < h; y0++) {
                for (int x0 = 0; x0 < w; x0++) {
                    var x1 = x0 + x;
                    var y1 = y0 + y;
                    q[project.p] = Math.clamp(plane + dw[x1] + dh[y1] - dp, 0, px);
                    q[project.x] = x1;
                    q[project.y] = y1;
                    writer.set(x0, y0, volume.get(q[0], q[2], q[1]));
                }
            }
        } else { // coronal, sagittal
            for (int x0 = 0; x0 < w; x0++) {
                for (int y0 = 0; y0 < h; y0++) {
                    var x1 = x0 + x;
                    var y1 = y0 + y;
                    q[project.p] = Math.clamp(plane + dw[x1] + dh[y1] - dp, 0, px);
                    q[project.x] = x1;
                    q[project.y] = y1;
                    writer.set(x0, y0, volume.get(q[0], q[2], q[1]));
                }
            }
        }

        return writer.get();
    }
}
