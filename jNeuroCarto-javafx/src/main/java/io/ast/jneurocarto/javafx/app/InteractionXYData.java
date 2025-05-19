package io.ast.jneurocarto.javafx.app;

import java.awt.geom.Point2D;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class InteractionXYData implements InteractionXYChart.PlottingJob {

    private static final Affine IDENTIFY = new Affine();
    private final InteractionXYChart<?> chart;
    private final Canvas canvas;
    private final boolean passive;
    private final GraphicsContext gc;
    private final List<XYSeries> data = new ArrayList<>();

    InteractionXYData(InteractionXYChart<?> chart, Canvas canvas, boolean passive) {
        this.chart = chart;
        this.canvas = canvas;
        this.passive = passive;
        gc = canvas.getGraphicsContext2D();
    }

    public boolean isPassive() {
        return passive;
    }

    /*==============*
     * carried data *
     *==============*/

    public static class XY {
        double x;
        double y;

        public XY(Point2D p) {
            this(p.getX(), p.getY());
        }

        public XY(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double x() {
            return x;
        }

        public void x(double x) {
            this.x = x;
        }

        public double y() {
            return y;
        }

        public void y(double y) {
            this.y = y;
        }
    }

    public static class XYSeries {
        public final String name;
        double z = 0;
        double w = 1;
        double h = 1;
        double lw = 1;
        @Nullable
        Color border = null;
        @Nullable
        Color fill = null;
        @Nullable
        Color line = null;
        boolean visible;

        private List<XY> data = new ArrayList<>();

        public XYSeries(String name) {
            this.name = name;
        }

        public String nName() {
            return name;
        }

        public int size() {
            return data.size();
        }

        public double z() {
            return z;
        }

        public void z(double z) {
            this.z = z;
        }

        public double w() {
            return w;
        }

        public void w(double w) {
            this.w = w;
        }

        public double h() {
            return h;
        }

        public void h(double h) {
            this.h = h;
        }

        public double linewidth() {
            return lw;
        }

        public void linewidth(double lw) {
            this.lw = lw;
        }

        /**
         * {@return border color of markers}
         */
        public @Nullable Color border() {
            return border;
        }

        public void border(@Nullable Color border) {
            this.border = border;
        }

        /**
         * {@return fill color of markers}
         */
        public @Nullable Color fill() {
            return fill;
        }

        public void fill(@Nullable Color fill) {
            this.fill = fill;
        }

        /**
         * {@return line color between markers}
         */
        public @Nullable Color line() {
            return line;
        }

        public XYSeries line(@Nullable Color line) {
            this.line = line;
            return this;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void paint(GraphicsContext gc) {
            if (!visible) return;

            var data = this.data;
            var length = data.size();
            var p = new double[2][];
            p[0] = new double[length];
            p[1] = new double[length];

            transform(gc.getTransform(), p);
            paint(gc, p, 0, length);
        }

        private int transform(Affine aff, double[][] p) {
            var data = this.data;
            var length = data.size();

            for (int i = 0; i < length; i++) {
                var xy = data.get(i);
                var q = aff.transform(xy.x, xy.y);
                p[0][i] = q.getX();
                p[1][i] = q.getY();
            }

            return length;
        }

        public void paint(GraphicsContext gc, XY[] xy) {
            if (!visible) return;

            var p = new double[2][];
            p[0] = new double[xy.length];
            p[1] = new double[xy.length];

            var aff = gc.getTransform();
            for (int i = 0, length = xy.length; i < length; i++) {
                var q = aff.transform(xy[i].x, xy[i].y);
                p[0][i] = q.getX();
                p[1][i] = q.getY();
            }

            paint(gc, p, 0, xy.length);
        }

        private void paint(GraphicsContext gc, double[][] p, int offset, int length) {
            gc.save();
            try {
                gc.setTransform(IDENTIFY);
                gc.setLineWidth(lw);
                if (line != null && length > 0) {
                    gc.setStroke(line);
                    gc.moveTo(p[0][offset], p[1][offset]);
                    for (int i = 1; i < length; i++) {
                        gc.lineTo(p[0][i + offset], p[1][i + offset]);
                    }
                    gc.stroke();
                }
                if (fill != null) {
                    gc.setFill(fill);
                    for (int i = 0; i < length; i++) {
                        gc.fillRect(p[0][i + offset], p[1][i + offset], w, h);
                    }
                }
                if (border != null) {
                    gc.setStroke(border);
                    for (int i = 0; i < length; i++) {
                        gc.strokeRect(p[0][i + offset], p[1][i + offset], w, h);
                    }
                }
            } finally {
                gc.restore();
            }
        }
    }

    /*==========*
     * plotting *
     *==========*/

    private @Nullable SoftReference<double[][]> transformedCache;

    public void repaint() {
        if (passive) return;
        clear();
        gc.setTransform(chart.getCanvasTransform());
        draw(gc);
    }

    public void clear() {
        if (passive) return;
        gc.setTransform(IDENTIFY);
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (data.isEmpty()) return;

        var length = data.stream().mapToInt(XYSeries::size).max().orElse(0);
        if (length == 0) return;

        var p = getTransformedCache(length);
        data.sort(Comparator.comparingDouble(XYSeries::z));

        var aff = gc.getTransform();
        for (var series : data) {
            var size = series.transform(aff, p);
            series.paint(gc, p, 0, size);
        }
    }

    private double[][] getTransformedCache(int length) {
        double[][] ret;
        if (transformedCache == null || (ret = transformedCache.get()) == null || ret[0].length < length) {
            ret = new double[2][];
            ret[0] = new double[length];
            ret[1] = new double[length];
            transformedCache = new SoftReference<>(ret);
        }
        return ret;
    }
}
