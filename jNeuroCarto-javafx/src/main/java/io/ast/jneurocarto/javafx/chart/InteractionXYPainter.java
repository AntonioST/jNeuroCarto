package io.ast.jneurocarto.javafx.chart;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class InteractionXYPainter implements InteractionXYChart.PlottingJob {

    static final Affine IDENTIFY = new Affine();
    private final InteractionXYChart chart;
    private final Canvas canvas;
    private final int layer;
    private final GraphicsContext gc;
    private final List<XYGraphics> graphics = new ArrayList<>();

    InteractionXYPainter(InteractionXYChart chart, Canvas canvas, int layer) {
        this.chart = chart;
        this.canvas = canvas;
        this.layer = layer;
        gc = canvas.getGraphicsContext2D();
    }

    /*========*
     * Series *
     *========*/

    public int numberOfGraphics() {
        return graphics.size();
    }

    public void addGraphics(XYGraphics graphic) {
        graphics.add(graphic);
        graphics.sort(Comparator.comparingDouble(XYGraphics::z));
    }

    public void addGraphics(Collection<XYGraphics> graphic) {
        this.graphics.addAll(graphic);
        graphics.sort(Comparator.comparingDouble(XYGraphics::z));
    }

    public boolean removeGraphics(XYGraphics graphic) {
        return graphics.remove(graphic);
    }

    public void removeGraphics(Collection<? extends XYGraphics> graphics) {
        this.graphics.removeAll(graphics);
    }

    public void clearGraphics() {
        graphics.clear();
    }


    /*==========*
     * plotting *
     *==========*/

    private @Nullable SoftReference<double[][]> transformedCache;

    public void repaint() {
        if (layer == 0) {
            clear();
            gc.setTransform(chart.getCanvasTransform());
            draw(gc);
        } else if (layer > 0) {
            chart.repaintForeground();
        } else {
            chart.repaintBackground();
        }
    }

    public void clear() {
        if (layer == 0) {
            gc.setTransform(IDENTIFY);
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (graphics.isEmpty()) return;

        var length = graphics.stream().mapToInt(XYGraphics::points).max().orElse(0);
        var p = getTransformedCache(length);

        var aff = gc.getTransform();
        for (var series : graphics) {
            if (series.isVisible()) {
                var size = series.transform(aff, p);
                series.paint(gc, p, 0, size);
            }
        }
    }

    private double[][] getTransformedCache(int length) {
        double[][] ret;
        if (transformedCache == null || (ret = transformedCache.get()) == null || ret[0].length < length) {
            ret = XYGraphics.createTransformedArray(length);
            transformedCache = new SoftReference<>(ret);
        }
        return ret;
    }

    /*===============*
     * data plotting *
     *===============*/

    /**
     * Create bar graphics.
     * {@snippet file = "BarExample.java" region = "bar example"}
     * <p/>
     * Create a stacked bar graphics.
     * {@snippet file = "BarExample.java" region = "stack bar example"}
     * <br/>
     * For a normalized stacked bar, add an extra line.
     * {@snippet file = "BarExample.java" region = "normalized stack bar example"}
     * <p/>
     * Note that once bar has stacked, they cannot add data or modify its position
     * (such as {@link XYBar.Builder#addData(double) addData} and {@link XYBar.Builder#step(double) step})
     * anymore.
     *
     * @param y
     * @return
     * @see io.ast.jneurocarto.javafx.cli.Bar#setup(InteractionXYChart)
     */
    public XYBar.Builder bar(double[] y) {
        return bar(y, XYBar.Orientation.vertical);
    }

    /**
     *
     * @param v
     * @param orientation
     * @return
     * @see #bar(double[])
     */
    public XYBar.Builder bar(double[] v, XYBar.Orientation orientation) {
        var ret = new XYBar();
        ret.orientation(orientation);
        addGraphics(ret);
        var builder = ret.builder(1);
        builder.addData(v);
        return builder;
    }

    public XYPath.Builder lines() {
        var ret = new XYPath();
        addGraphics(ret);
        return ret.builder();
    }

    public XYPath.Builder lines(double[] y) {
        var ret = new XYPath();
        for (int i = 0, length = y.length; i < length; i++) {
            ret.addData(i, y[i]);
        }
        addGraphics(ret);
        return ret.builder();
    }

    public XYPath.Builder lines(double[] x, double[] y) {
        int length = x.length;
        if (length != y.length) throw new IllegalArgumentException();

        var ret = new XYPath();
        for (int i = 0; i < length; i++) {
            ret.addData(x[i], y[i]);
        }
        addGraphics(ret);
        return ret.builder();
    }

    public XYMarker.Builder scatter() {
        var ret = new XYMarker();
        addGraphics(ret);
        return ret.builder();
    }

    public XYMarker.Builder scatter(double[] y) {
        var ret = new XYMarker();
        for (int i = 0, length = y.length; i < length; i++) {
            ret.addData(i, y[i]);
        }
        addGraphics(ret);
        return ret.builder();
    }

    public XYMarker.Builder scatter(double[] x, double[] y) {
        int length = x.length;
        if (length != y.length) throw new IllegalArgumentException();

        var ret = new XYMarker();
        for (int i = 0; i < length; i++) {
            ret.addData(x[i], y[i]);
        }
        addGraphics(ret);
        return ret.builder();
    }

    public XYMatrix.Builder imshow() {
        var ret = new XYMatrix();
        addGraphics(ret);
        return ret.builder();
    }

    public XYMatrix.Builder imshow(double[][] mat) {
        return imshow(mat, false);
    }

    public XYMatrix.Builder imshow(double[][] mat, boolean flip) {
        var ret = new XYMatrix();
        ret.addData(mat, flip);
        addGraphics(ret);
        return ret.builder();
    }

    public XYMatrix.Builder imshow(double[] mat, int row) {
        return imshow(mat, row, false);
    }

    public XYMatrix.Builder imshow(double[] mat, int row, boolean flip) {
        var ret = new XYMatrix();
        ret.addData(mat, row, flip);
        addGraphics(ret);
        return ret.builder();
    }
}
