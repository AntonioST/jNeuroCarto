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
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.javafx.app.InteractionXYChart;

@NullMarked
public class SlicePainter implements InteractionXYChart.PlottingJob {

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

    public final DoubleProperty imageAlpha = new SimpleDoubleProperty(1);

    public final double getImageAlpha() {
        return imageAlpha.get();
    }

    public final void setImageAlpha(double alpha) {
        if (!(0 <= alpha && alpha <= 1)) throw new IllegalArgumentException();
        imageAlpha.set(alpha);
    }

    public final BooleanProperty drawAtlasBrainBoundary = new SimpleBooleanProperty(false);

    public final boolean isDrawAtlasBrainBoundary() {
        return drawAtlasBrainBoundary.get();
    }

    public final void setDrawAtlasBrainBoundary(boolean value) {
        drawAtlasBrainBoundary.set(value);
    }

    public final BooleanProperty drawAtlasBrainImage = new SimpleBooleanProperty(true);

    public final boolean isDrawAtlasBrainImage() {
        return drawAtlasBrainImage.get();
    }

    public final void setDrawAtlasBrainImage(boolean value) {
        drawAtlasBrainImage.set(value);
    }

    public final BooleanProperty visible = new SimpleBooleanProperty(true);

    public boolean isVisible() {
        return visible.get();
    }

    public void setVisible(boolean value) {
        visible.set(value);
    }

    /*===========*
     * transform *
     *===========*/

    private static final Affine AFF_FLIP_LR = new Affine(-1, 0, 0, 0, 1, 0);
    private static final Affine AFF_FLIP_UP = new Affine(1, 0, 0, 0, -1, 0);

    /**
     * {@return an affine transform from slice to chart coordinate system.}
     */
    public Affine getChartTransform() {
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

    private @Nullable ImageSlice sliceCache;
    private @Nullable Image imageCache;

    public void update(ImageSlice slice) {
        if (!Objects.equals(slice, sliceCache)) {
            sliceCache = slice;
            imageCache = slice.imageFx();
        }
    }

    public Bounds getBound() {
        var w = (sliceCache == null) ? 0 : sliceCache.width();
        var h = (sliceCache == null) ? 0 : sliceCache.height();

        Bounds ret = new BoundingBox(0, 0, w, h);
        ret = getChartTransform().transform(ret);
        return ret;
    }

    public void draw(GraphicsContext gc) {
        if (!isVisible()) return;

        var slice = sliceCache;
        if (slice == null) return;
        var image = imageCache;
        if (image == null) return;

        var w = slice.width();
        var h = slice.height();
        var aff = gc.getTransform();
        gc.save();
        try {
            aff.append(getChartTransform());
            gc.setTransform(aff);

            if (isDrawAtlasBrainImage()) {
                drawImage(gc, image, w, h);
            }
            if (isDrawAtlasBrainBoundary()) {
                drawBounds(gc, slice);
            }
        } finally {
            gc.restore();
        }
    }

    private void drawImage(GraphicsContext gc, Image image, double w, double h) {
        gc.setGlobalAlpha(getImageAlpha());
        gc.drawImage(image, 0, 0, w, h);
    }

    private void drawBounds(GraphicsContext gc, ImageSlice slice) {
        var w = slice.width();
        var h = slice.height();

        gc.setGlobalAlpha(1);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, w, h);
        gc.strokeLine(0, 0, w, h);
        gc.strokeLine(0, h, w, 0);
    }
}
