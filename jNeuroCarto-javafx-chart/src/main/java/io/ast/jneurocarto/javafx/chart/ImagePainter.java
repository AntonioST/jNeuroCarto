package io.ast.jneurocarto.javafx.chart;

import javafx.beans.property.*;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ImagePainter implements InteractionXYChart.PlottingJob {

    /*============*
     * properties *
     *============*/

    public final ObjectProperty<@Nullable Image> imageProperty = new SimpleObjectProperty<>();

    public final @Nullable Image getImage() {
        return imageProperty.get();
    }

    public final void setImage(@Nullable Image value) {
        imageProperty.set(value);
    }

    /**
     * image x position in chart.
     */
    public final DoubleProperty x = new SimpleDoubleProperty();

    public final double x() {
        return x.get();
    }

    public final void x(double x) {
        this.x.set(x);
    }

    /**
     * image y position in chart.
     */
    public final DoubleProperty y = new SimpleDoubleProperty();

    public final double y() {
        return y.get();
    }

    public final void y(double y) {
        this.y.set(y);
    }

    /**
     * offset image position.
     *
     * @param dx x offset in chart.
     * @param dy y offset in chart.
     */
    public void translate(double dx, double dy) {
        x.set(x.get() + dx);
        y.set(y.get() + dy);
    }

    /**
     * image width in chart.
     */
    public final DoubleProperty width = new SimpleDoubleProperty();

    public final double width() {
        return width.get();
    }

    public final void width(double value) {
        width.set(value);
    }

    /**
     * image height in chart.
     */
    public final DoubleProperty height = new SimpleDoubleProperty();

    public final double height() {
        return height.get();
    }

    public final void height(double value) {
        height.set(value);
    }

    /**
     * image scaling on x-axis with unit: canvas/image
     */
    public final DoubleProperty sx = new SimpleDoubleProperty(1);

    /**
     * image scaling on y-axis with unit: canvas/image
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
     * {@return an affine transform from image to chart coordinate system.}
     */
    public Affine getChartTransform() {
        var x = x();
        var y = y();
        var w = width.get();
        var h = height.get();
        var cx = w / 2;
        var cy = h / 2;

        var r = r();
        if (invertRotation()) r = -r;

        var aff = new Affine();
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
     * {@return an affine transform from chart to image coordinate system.}
     */
    public Affine getImageTransform() {
        try {
            return getChartTransform().createInverse();
        } catch (NonInvertibleTransformException e) {
            throw new RuntimeException(e);
        }
    }

    public Bounds getBound() {
        var w = width.get();
        var h = height.get();

        Bounds ret = new BoundingBox(0, 0, w, h);
        ret = getChartTransform().transform(ret);
        return ret;
    }

    /*===============*
     * image painter *
     *===============*/

    @Override
    public void draw(GraphicsContext gc) {
        if (!isVisible()) return;

        var image = imageProperty.get();
        if (image == null) return;

        var w = width.get();
        var h = height.get();

        gc.save();
        try {
            gc.transform(getChartTransform());

            if (isDrawAtlasBrainImage()) {
                gc.setGlobalAlpha(getImageAlpha());
                gc.drawImage(image, 0, 0, w, h);
            }
            if (isDrawAtlasBrainBoundary()) {
                var b = gc.getTransform().transform(new BoundingBox(0, 0, w, h));
                gc.setTransform(new Affine());
                gc.setGlobalAlpha(1);
                gc.setStroke(Color.RED);
                gc.setLineWidth(2);

                var x = b.getMinX();
                var y = b.getMinY();
                w = b.getWidth();
                h = b.getHeight();
                gc.strokeRect(x, y, w, h);
                gc.strokeLine(x, y, x + w, y + h);
                gc.strokeLine(x, y + h, x + w, y);
            }
        } finally {
            gc.restore();
        }
    }
}
