package io.ast.jneurocarto.javafx.atlas;

import java.util.Objects;

import io.ast.jneurocarto.atlas.ImageSlice;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class SlicePainter {

    /*============*
     * properties *
     *============*/

    /**
     * image position in canvas.
     */
    public final DoubleProperty x = new SimpleDoubleProperty();

    public final double x() {
        return x.get();
    }

    public final void x(double x) {
        this.x.set(x);
    }

    /**
     * image position in canvas.
     */
    public final DoubleProperty y = new SimpleDoubleProperty();

    public final double y() {
        return y.get();
    }

    public final void y(double y) {
        this.y.set(y);
    }

    /**
     * image scaling with unit: canvas[px]/image[px]
     */
    public final DoubleProperty s = new SimpleDoubleProperty(1);

    public double s() {
        return s.get();
    }

    public void s(double s) {
        this.s.set(s);
    }

    /**
     * rotation degrees
     */
    public final DoubleProperty r = new SimpleDoubleProperty();

    public double r() {
        return r.get();
    }

    public void r(double r) {
        this.r.set(r);
    }

    /**
     * rotation anchor x position in image[px].
     */
    public final DoubleProperty ax = new SimpleDoubleProperty();

    public double ax() {
        return ax.get();
    }

    public void ax(double ax) {
        this.ax.set(ax);
    }

    /**
     * rotation anchor y position in image[px].
     */
    public final DoubleProperty ay = new SimpleDoubleProperty();

    public double ay() {
        return ay.get();
    }

    public void ay(double ay) {
        this.ay.set(ay);
    }

    /*======*
     * draw *
     *======*/

    private ImageSlice sliceCache;
    private Image imageCache;

    public void draw(GraphicsContext gc, ImageSlice slice) {
        Image image;
        if (Objects.equals(slice, sliceCache)) {
            image = imageCache;
        } else {
            image = imageCache = slice.imageFx();
            sliceCache = slice;
        }

        var x = x();
        var y = y();
        var s = s();
        var w = slice.widthPx() * s;
        var h = slice.heightPx() * s;

        var r = r();
        if (r == 0.0) {
            gc.drawImage(image, x, y, w, h);
        } else {
            gc.save();
            try {
                var ax = ax() * s;
                var ay = ay() * s;
                gc.translate(ax, ay);
                gc.rotate(-r);
                gc.translate(-ax, -ay);
                gc.drawImage(image, x, y, w, h);
            } finally {
                gc.restore();
            }
        }
    }
}
