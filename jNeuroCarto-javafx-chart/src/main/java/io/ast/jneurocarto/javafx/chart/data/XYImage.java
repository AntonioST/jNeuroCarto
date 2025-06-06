package io.ast.jneurocarto.javafx.chart.data;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.Nullable;

/// Show image in [InteractionXYChart][io.ast.jneurocarto.javafx.chart.InteractionXYChart].
/// It is different from [ImagePainter][io.ast.jneurocarto.javafx.chart.ImagePainter], due
/// to following differences:
///
/// 1. This doesn't care about the coordinate transformation between image and chart.
///     The transform only used for plotting.
/// 2. The transformation origin, especially for rotation and scaling are image's origin,
///     rather the center of the image in [ImagePainter][io.ast.jneurocarto.javafx.chart.ImagePainter].
public final class XYImage {

    private static final Affine AFF_FLIP_LR = new Affine(-1, 0, 0, 0, 1, 0);
    private static final Affine AFF_FLIP_UP = new Affine(1, 0, 0, 0, -1, 0);

    public double x;
    public double y;
    private double z;
    public double w;
    public double h;
    public double sx = 1;
    public double sy = 1;
    public double r;
    public boolean flipLR;
    public boolean flipUD = true;
    public double alpha = 1;

    public @Nullable Image image;
    @Nullable
    XYImageLayer layer;

    public XYImage(Image image) {
        this.image = image;
    }

    public XYImage(XYImage image) {
        this.x = image.x;
        this.y = image.y;
        this.z = image.z;
        this.w = image.w;
        this.h = image.h;
        this.sx = image.sx;
        this.sy = image.sy;
        this.r = image.r;
        this.flipUD = image.flipUD;
        this.flipLR = image.flipLR;
        this.alpha = image.alpha;
        this.image = image.image;
    }

    public XYImage(Image image, double x, double y) {
        this.image = image;
        this.x = x;
        this.y = y;
    }

    public XYImage(Image image, double x, double y, double w, double h) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public double z() {
        return z;
    }

    public void z(double z) {
        this.z = z;
        if (layer != null) {
            layer.dirtyZ();
        }
    }

    public void move(Point2D p) {
        x += p.getX();
        y += p.getY();
    }

    public void moveTo(Point2D p) {
        x = p.getX();
        y = p.getY();
    }

    public void moveTo(Bounds b) {
        x = b.getMinX();
        y = b.getMinY();
        w = b.getWidth();
        h = b.getHeight();
    }

    public void scale(double s) {
        sx = s;
        sy = s;
    }

    public void scale(double sx, double sy) {
        this.sx = sx;
        this.sy = sy;
    }

    public void paint(GraphicsContext gc) {
        if (image == null) return;

        var w = this.w > 0 ? this.w : image.getWidth();
        var h = this.h > 0 ? this.h : image.getHeight();
        var cx = w / 2;
        var cy = h / 2;

        gc.save();
        try {
            gc.setGlobalAlpha(alpha);
            gc.translate(x, y);
            if (sx != 1 || sy != 1) gc.scale(sx, sy);
            if (r != 0) gc.rotate(r);
            if (flipUD || flipLR) {
                gc.translate(cx, cy);
                if (flipUD) gc.transform(AFF_FLIP_UP);
                if (flipLR) gc.transform(AFF_FLIP_LR);
                gc.translate(-cx, -cy);
            }

            gc.drawImage(image, 0, 0, w, h);
        } finally {
            gc.restore();
        }
    }
}
