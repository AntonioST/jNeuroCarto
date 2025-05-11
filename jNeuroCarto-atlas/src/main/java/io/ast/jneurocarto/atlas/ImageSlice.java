package io.ast.jneurocarto.atlas;

import java.awt.image.BufferedImage;

import org.jspecify.annotations.NullMarked;

/**
 * @param plane
 * @param ax    anchor x position
 * @param ay    anchor y position
 * @param dw    offset at width-edge
 * @param dh    offset at width-edge
 * @param slice
 */
@NullMarked
public record ImageSlice(int plane, int ax, int ay, int dw, int dh, ImageSlices slice) {

    public ImageSlices.View view() {
        return slice.view();
    }

    /**
     * {@return resolution double array on (p, x, y) axis}
     */
    public double[] resolution() {
        return slice.resolution();
    }

    public double width() {
        return slice.widthUm();
    }

    public double height() {
        return slice.heightUm();
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
     * @return return {@code coor} but updating {@link ImageSlices.CoordinateIndex#p()} as result.
     */
    public ImageSlices.CoordinateIndex planeAt(ImageSlices.CoordinateIndex coor) {
        var p = planeAt(coor.x(), coor.y());
        return new ImageSlices.CoordinateIndex(p, coor.x(), coor.y());
    }

    /**
     * @param coor
     * @return return {@code coor} but updating {@link ImageSlices.Coordinate#p()} as result.
     */
    public ImageSlices.Coordinate planeAt(ImageSlices.Coordinate coor) {
        var p = planeAt(coor.x(), coor.y());
        return new ImageSlices.Coordinate(p, coor.x(), coor.y());
    }

    /**
     * project coordinate (AP, DV, ML) into (p, x, y)
     *
     * @param coor coordinate (AP, DV, ML)
     * @return coordinate (p, x, y),
     * where {@link ImageSlices.CoordinateIndex#p()} will be replaced by {@link #planeAt(ImageSlices.CoordinateIndex)}.
     */
    public ImageSlices.Coordinate project(BrainAtlas.Coordinate coor) {
        return planeAt(slice.project(coor));
    }

    /**
     * project coordinate (AP, DV, ML) into (p, x, y)
     *
     * @param coor coordinate (AP, DV, ML)
     * @return coordinate (p, x, y),
     * where {@link ImageSlices.CoordinateIndex#p()} will be replaced by {@link #planeAt(ImageSlices.CoordinateIndex)}.
     */
    public ImageSlices.CoordinateIndex project(BrainAtlas.CoordinateIndex coor) {
        return planeAt(slice.project(coor));
    }

    /**
     * project coordinate (x, y) into (AP, DV, ML)
     *
     * @param coor coordinate (p, x, y),
     *             where {@link ImageSlices.Coordinate#p()} will be replaced by {@link #planeAt(ImageSlices.Coordinate)}.
     * @return coordinate (AP, DV, ML)
     */
    public BrainAtlas.Coordinate pullBack(ImageSlices.Coordinate coor) {
        return slice.pullBack(planeAt(coor));
    }

    /**
     * project coordinate (x, y) into (AP, DV, ML)
     *
     * @param coor coordinate (p, x, y),
     *             where {@link ImageSlices.CoordinateIndex#p()} will be replaced by {@link #planeAt(ImageSlices.CoordinateIndex)}.
     * @return coordinate (AP, DV, ML)
     */
    public BrainAtlas.CoordinateIndex pullBack(ImageSlices.CoordinateIndex coor) {
        return slice.pullBack(planeAt(coor));
    }

    /**
     * get rotation on (ap, dv, ml).
     *
     * @return rotation on (ap, dv, ml). Reuse {@link BrainAtlas.Coordinate} but changing fields' meaning to roration radians.
     */
    public BrainAtlas.Coordinate offset2Angle() {
        return slice.offset2Angle(dw, dh);
    }

    public ImageSlice withPlane(int plane) {
        return new ImageSlice(plane, ax, ay, dw, dh, slice);
    }

    public ImageSlice withAnchor(int ax, int ay) {
        var plane = planeAt(ax, ay);
        return new ImageSlice(plane, ax, ay, dw, dh, slice);
    }

    public ImageSlice withOffset(int dw, int dh) {
        return new ImageSlice(plane, ax, ay, dw, dh, slice);
    }

    public ImageSlice withRotate(int rx, int ry) {
        var dw = -width() * Math.tan(ry) / 2;
        var dh = height() * Math.tan(rx) / 2;
        var r = resolution();
        return new ImageSlice(plane, ax, ay, (int) (dw / r[1]), (int) (dh / r[2]), slice);
    }

    public BufferedImage image() {
        var resolution = resolution();
        var rp = resolution[0];
        var rx = resolution[1];
        var ry = resolution[2];

        var width = slice.width();
        var height = slice.height();

        var cx = width * rx / 2;
        var cy = height * ry / 2;

        var volume = slice.getVolume();
        var image = new BufferedImage(width, height, volume.type);

        var dw = new int[width];
        var dh = new int[height];

        var fw = this.dw * rx / cx;
        var fh = this.dh * ry / cy;
        for (int w = 0; w < width; w++) {
            dw[w] = (int) (fw / (w * rx - cx) / rp);
        }
        for (int h = 0; h < height; h++) {
            dh[h] = (int) (fh / (h * ry - cy) / rp);
        }

        var px = slice.plane();

        var project = view();
        var q = new int[3];
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                q[project.p] = Math.clamp(plane + dw[w] + dh[h], 0, px);
                q[project.x] = w;
                q[project.y] = h;
                image.setRGB(w, h, volume.get(q[0], q[1], q[2]));
            }
        }
        return image;
    }
}
