package io.ast.jneurocarto.javafx.atlas;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.transform.Affine;

import io.ast.jneurocarto.atlas.ImageSlice;

public class SlicePainter {

    /*============*
     * properties *
     *============*/

    /**
     * image position in chart.
     */
    public final DoubleProperty x = new SimpleDoubleProperty();

    public final double x() {
        return x.get();
    }

    public final void x(double x) {
        this.x.set(x);
    }

    /**
     * image position in chart.
     */
    public final DoubleProperty y = new SimpleDoubleProperty();

    public final double y() {
        return y.get();
    }

    public final void y(double y) {
        this.y.set(y);
    }

    /**
     * @param dx x offset in chart.
     * @param dy y offset in chart.
     */
    public void translate(double dx, double dy) {
        x.set(x.get() + dx);
        y.set(y.get() + dy);
    }

    /**
     * image scaling on x-axis with unit: canvas[px]/image[px]
     */
    public final DoubleProperty sx = new SimpleDoubleProperty(1);

    /**
     * image scaling on y-axis with unit: canvas[px]/image[px]
     */
    public final DoubleProperty sy = new SimpleDoubleProperty(1);

    public double sx() {
        return sx.get();
    }

    public double sy() {
        return sy.get();
    }

    public void s(double s) {
        this.sx.set(s);
        this.sy.set(s);
    }

    public void sx(double s) {
        this.sx.set(s);
    }

    public void sy(double s) {
        this.sy.set(s);
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

    /**
     * flip left-side right
     */
    public final BooleanProperty flipLR = new SimpleBooleanProperty();

    public boolean flipLR() {
        return flipLR.get();
    }

    public void flipLR(boolean value) {
        flipLR.set(value);
    }

    /**
     * flip upside down
     */
    public final BooleanProperty flipUD = new SimpleBooleanProperty();

    public boolean flipUD() {
        return flipUD.get();
    }

    public void flipUD(boolean value) {
        flipUD.set(value);
    }

    /*===========*
     * transform *
     *===========*/

    /**
     * {@return an affine transform from chart to slice coordinate system.}
     */
    public Affine getSliceTransform() {
        //XXX Unsupported Operation SlicePainter.getSliceTransform
        throw new UnsupportedOperationException();
    }

    /**
     * {@return an affine transform from slice to chart coordinate system.}
     */
    public Affine getChartTransform() {
        var ax = ax() * sx();
        var ay = ay() * sy();

        var aff = new Affine();
        aff.appendRotation(r(), ax, ay);
        return aff;
    }

    /*======*
     * draw *
     *======*/

    private ImageSlice sliceCache;
    private Image imageCache;

    public Bounds getBound(ImageSlice slice) {
        return getBound(slice, false);
    }

    public Bounds getBound(ImageSlice slice, boolean chartCoor) {
        var x = x();
        var y = y();
        var w = slice.widthPx() * sx();
        var h = slice.heightPx() * sy();

        Bounds ret = new BoundingBox(x, y, w, h);
        if (chartCoor) {
            ret = getChartTransform().transform(ret);
        }
        return ret;
    }

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
        var w = slice.widthPx() * sx();
        var h = slice.heightPx() * sy();

        if (flipUD()) {
            y += h;
            h = -h;
        }
        if (flipLR()) {
            x += w;
            w = -w;
        }

        var r = r();
        if (r == 0.0) {
            gc.drawImage(image, x, y, w, h);
        } else {
            gc.save();
            try {
                var ax = ax() * sx();
                var ay = ay() * sy();
                gc.translate(ax, ay);
                gc.rotate(-r);
                gc.translate(-ax, -ay);
                gc.drawImage(image, x, y, w, h);
            } finally {
                gc.restore();
            }
        }
    }

    public void drawBounds(GraphicsContext gc, ImageSlice slice, boolean crossing) {
        var x = x();
        var y = y();
        var w = slice.widthPx() * sx();
        var h = slice.heightPx() * sy();

        var r = r();
        gc.save();
        try {
            var ax = ax() * sx();
            var ay = ay() * sy();
            gc.translate(ax, ay);
            gc.rotate(-r);
            gc.translate(-ax, -ay);
            gc.strokeRect(x, y, w, h);
            if (crossing) {
                gc.strokeLine(x, y, x + w, y + h);
                gc.strokeLine(x, y + h, x + w, y);
            }
        } finally {
            gc.restore();
        }
    }


}
