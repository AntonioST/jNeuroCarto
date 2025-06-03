package io.ast.jneurocarto.atlas;

import java.awt.image.BufferedImage;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.CoordinateIndex;

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
     * @param coor coordinate (p, x, y),
     *             where {@link SliceCoordinate#p()} will be replaced by {@link #planeAt(SliceCoordinate)}.
     * @return coordinate (AP, DV, ML)
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

    public static final ImageWriter<BufferedImage> AWT_IMAGE = new BufferedImageWriter();
    public static final ImageWriter<Image> JFX_IMAGE = new JavaFxImageWriter();

    public sealed interface ImageWriter<T> {
        void create(int w, int h);

        void set(int w, int h, int v);

        T get();
    }

    private static final class BufferedImageWriter implements ImageWriter<BufferedImage> {
        private BufferedImage image;

        @Override
        public void create(int w, int h) {
            image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        }

        @Override
        public void set(int w, int h, int v) {
            image.setRGB(w, h, v);
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
        public void create(int w, int h) {
            image = new WritableImage(w, h);
            writer = image.getPixelWriter();
        }

        @Override
        public void set(int w, int h, int v) {
            writer.setArgb(w, h, v);
        }

        @Override
        public Image get() {
            return image;
        }
    }

    public <T> T image(ImageWriter<T> writer) {
        var resolution = resolution();
        var rp = resolution[0];
        var rx = resolution[1];
        var ry = resolution[2];

        var plane = this.plane;
        var width = stack.width();
        var height = stack.height();

        var cx = width * rx / 2;
        var cy = height * ry / 2;

        var volume = stack.getVolume();
        writer.create(width, height);

        var dw = new int[width];
        var dh = new int[height];

        var fw = this.dw * rx / cx;
        var fh = this.dh * ry / cy;
        for (int w = 0; w < width; w++) {
            dw[w] = (int) (fw * (w * rx - cx) / rp);
        }
        for (int h = 0; h < height; h++) {
            dh[h] = (int) (fh * (h * ry - cy) / rp);
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
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    q[project.p] = Math.clamp(plane + dw[w] + dh[h] - dp, 0, px);
                    q[project.x] = w;
                    q[project.y] = h;
                    writer.set(w, h, volume.get(q[0], q[2], q[1]));
                }
            }
        } else { // coronal, sagittal
            for (int w = 0; w < width; w++) {
                for (int h = 0; h < height; h++) {
                    q[project.p] = Math.clamp(plane + dw[w] + dh[h] - dp, 0, px);
                    q[project.x] = w;
                    q[project.y] = h;
                    writer.set(w, h, volume.get(q[0], q[2], q[1]));
                }
            }
        }

        return writer.get();
    }
}
