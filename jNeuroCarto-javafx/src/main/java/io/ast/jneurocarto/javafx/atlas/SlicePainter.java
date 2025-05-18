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
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.Nullable;

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

    /**
     * invert rotation direction
     */
    public final BooleanProperty invertRotation = new SimpleBooleanProperty();

    public boolean invertRotation() {
        return invertRotation.get();
    }

    public void invertRotation(boolean value) {
        invertRotation.set(value);
    }

    /*===========*
     * transform *
     *===========*/

    private static final Affine AFF_FLIP_LR = new Affine(-1, 0, 0, 0, 1, 0);
    private static final Affine AFF_FLIP_UP = new Affine(1, 0, 0, 0, -1, 0);

    private @Nullable Affine affineCache;

    /**
     * {@return an affine transform from slice to chart coordinate system.}
     */
    public Affine getChartTransform() {
        if (affineCache != null) return affineCache;

        var x = x();
        var y = y();
        var w = (sliceCache == null) ? 0 : sliceCache.width();
        var h = (sliceCache == null) ? 0 : sliceCache.height();
        var cx = w / 2;
        var cy = h / 2;

        var r = r();
        if (invertRotation()) r = -r;

        var aff = new Affine();
        // A S T Fx Fy R
        aff.appendScale(sx(), sy());
        aff.appendTranslation(x, y);
        if (flipUD()) {
            aff.appendTranslation(cx, cy);
            aff.append(AFF_FLIP_UP);
            aff.appendTranslation(-cx, -cy);
        }
        if (flipLR()) {
            aff.appendTranslation(cx, cy);
            aff.append(AFF_FLIP_LR);
            aff.appendTranslation(-cx, -cy);
        }
        aff.appendRotation(r, cx, cy);
        affineCache = aff;
        return aff;
    }

    /**
     * {@return an affine transform from chart to slice coordinate system.}
     */
    public Affine getSliceTransform() {
        try {
            return getChartTransform().createInverse();
        } catch (NonInvertibleTransformException e) {
            throw new RuntimeException(e);
        }
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
            ret = getSliceTransform().transform(ret);
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
            affineCache = null;
        }

        var w = slice.width();
        var h = slice.height();
        var aff = gc.getTransform();
        gc.save();
        try {
            aff.append(getChartTransform());
            gc.setTransform(aff);
            gc.drawImage(image, 0, 0, w, h);
        } finally {
            gc.restore();
        }
    }

    public void drawBounds(GraphicsContext gc, ImageSlice slice) {
        if (!Objects.equals(slice, sliceCache)) {
            sliceCache = slice;
            affineCache = null;
        }

        var w = slice.width();
        var h = slice.height();
        var aff = gc.getTransform();
        gc.save();
        try {
            aff.append(getChartTransform());
            gc.setTransform(aff);
            gc.strokeRect(0, 0, w, h);
            gc.strokeLine(0, 0, w, h);
            gc.strokeLine(0, h, w, 0);
        } finally {
            gc.restore();
        }
    }


}
