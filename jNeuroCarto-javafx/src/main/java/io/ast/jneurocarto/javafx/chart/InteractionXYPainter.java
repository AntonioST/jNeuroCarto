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
    private final InteractionXYChart<?> chart;
    private final Canvas canvas;
    private final int layer;
    private final GraphicsContext gc;
    private final List<XYGraphics> graphics = new ArrayList<>();

    InteractionXYPainter(InteractionXYChart<?> chart, Canvas canvas, int layer) {
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
}
