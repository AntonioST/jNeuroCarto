package io.ast.jneurocarto.javafx.chart;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class InteractionXYPainter implements InteractionXYChart.PlottingJob {

    private static final Affine IDENTIFY = new Affine();
    private final InteractionXYChart<?> chart;
    private final Canvas canvas;
    private final int layer;
    private final GraphicsContext gc;
    private final List<XYSeries> data = new ArrayList<>();

    InteractionXYPainter(InteractionXYChart<?> chart, Canvas canvas, int layer) {
        this.chart = chart;
        this.canvas = canvas;
        this.layer = layer;
        gc = canvas.getGraphicsContext2D();
    }

    /*==============*
     * carried data *
     *==============*/

    public static class XY {
        static final XY GAP = new XY(Double.NaN, Double.NaN, null);

        double x;
        double y;
        final @Nullable Object external;

        public XY(Point2D p) {
            this(p.getX(), p.getY(), null);
        }

        public XY(double x, double y) {
            this(x, y, null);
        }

        public XY(Point2D p, @Nullable Object external) {
            this(p.getX(), p.getY(), external);
        }

        public XY(double x, double y, @Nullable Object external) {
            this.x = x;
            this.y = y;
            this.external = external;
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

        public boolean isGap() {
            return Double.isNaN(x) || Double.isNaN(y);
        }

        public @Nullable Object external() {
            return external;
        }

        @Override
        public String toString() {
            return "XY{" + x + "," + y + '}';
        }
    }

    public static class XYSeries {
        public final String name;
        private double z = 0;
        private double w = 1;
        private double h = 1;
        private double lw = 1;
        private double alpha = 1;
        private @Nullable Color markerEdge = null;
        private @Nullable Color marker = null;
        private @Nullable Color line = null;
        private @Nullable Color fill = null;
        private boolean visible = true;

        private List<XY> data = new ArrayList<>();

        public XYSeries(String name) {
            this.name = name;
        }

        public String name() {
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

        public double alpha() {
            return alpha;
        }

        public void alpha(double alpha) {
            this.alpha = alpha;
        }

        /**
         * {@return border color of markers}
         */
        public @Nullable Color markerEdge() {
            return markerEdge;
        }

        public void markerEdge(@Nullable Color border) {
            this.markerEdge = border;
        }

        /**
         * {@return fill color of markers}
         */
        public @Nullable Color marker() {
            return marker;
        }

        public void marker(@Nullable Color fill) {
            this.marker = fill;
        }

        /**
         * {@return fill color inside lines}
         */
        public @Nullable Color fill() {
            return fill;
        }

        public XYSeries fill(@Nullable Color fill) {
            this.fill = fill;
            return this;
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

        public Stream<XY> data() {
            return data.stream();
        }

        public void clearData() {
            data.clear();
        }

        public void addGap() {
            data.add(XY.GAP);
        }

        public XY addData(double x, double y) {
            var ret = new XY(x, y);
            data.add(ret);
            return ret;
        }

        public XY addData(Point2D p) {
            var ret = new XY(p);
            data.add(ret);
            return ret;
        }

        public void addData(XY data) {
            this.data.add(data);
        }

        public void addData(List<XY> data) {
            this.data.addAll(data);
        }

        public void addData(Stream<XY> data) {
            data.forEach(this.data::add);
        }

        public void copyData(XYSeries other) {
            other.data.addAll(data);
        }

        public void copyData(XYSeries other, Predicate<XY> tester) {
            for (XY xy : data) {
                if (tester.test(xy)) {
                    other.addData(xy);
                }
            }
        }

        public void transferData(XYSeries other) {
            other.data.addAll(data);
            data.clear();
        }

        public void transferData(XYSeries other, Predicate<XY> tester) {
            var iter = data.iterator();
            while (iter.hasNext()) {
                var xy = iter.next();
                if (tester.test(xy)) {
                    other.addData(xy);
                    iter.remove();
                }
            }
        }

        public XY removeData(int index) {
            return data.remove(index);
        }

        public List<XY> removeData(int index, int length) {
            var ret = new ArrayList<XY>(length);
            var iter = data.listIterator(index);
            for (int i = 0; i < length; i++) {
                ret.add(iter.next());
                iter.remove();
            }
            return ret;
        }

        public List<XY> removeData(Predicate<XY> tester) {
            var ret = new ArrayList<XY>(size());
            var iter = data.iterator();
            while (iter.hasNext()) {
                var xy = iter.next();
                if (tester.test(xy)) {
                    ret.add(xy);
                    iter.remove();
                }
            }
            return ret;
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
                gc.setGlobalAlpha(alpha);
                gc.setLineWidth(lw);
                if ((line != null || fill != null) && length > 0) {
                    paintLineAndFill(gc, p, offset, length);
                }

                paintMarkers(gc, p, offset, length);
            } finally {
                gc.restore();
            }
        }

        private void paintLineAndFill(GraphicsContext gc, double[][] p, int offset, int length) {
            var counter = 0;

            for (int i = 0; i < length; i++) {
                var j = i + offset;

                var x = p[0][j];
                var y = p[1][j];
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    counter = 0;

                    if (fill != null) {
                        gc.setFill(fill);
                        gc.fill();
                    }
                    if (line != null) {
                        gc.setStroke(line);
                        gc.stroke();
                    }

                } else if (counter == 0) {
                    gc.beginPath();
                    gc.moveTo(x, y);
                    counter++;
                } else {
                    gc.lineTo(x, y);
                    counter++;
                }
            }

            if (counter > 1) {
                if (fill != null) {
                    gc.setFill(line);
                    gc.fill();
                }
                if (line != null) {
                    gc.setStroke(line);
                    gc.stroke();
                }
            }
        }

        private void paintMarkers(GraphicsContext gc, double[][] p, int offset, int length) {
            var dx = w / 2;
            var dy = h / 2;

            if (marker != null) {
                gc.setFill(marker);
                for (int i = 0; i < length; i++) {
                    var x = p[0][i + offset];
                    var y = p[1][i + offset];
                    if (!Double.isNaN(x) && !Double.isNaN(y)) {
                        gc.fillRect(x - dx, y - dy, w, h);
                    }
                }
            }

            if (markerEdge != null) {
                gc.setStroke(markerEdge);
                for (int i = 0; i < length; i++) {
                    var x = p[0][i + offset];
                    var y = p[1][i + offset];
                    if (!Double.isNaN(x) && !Double.isNaN(y)) {
                        gc.strokeRect(x - dx, y - dy, w, h);
                    }
                }
            }
        }
    }

    /*========*
     * Series *
     *========*/

    public int seriesNumber() {
        return data.size();
    }

    public List<String> listSeriesName() {
        return data.stream().map(XYSeries::name).toList();
    }

    public XYSeries addSeries(String name) {
        var ret = new XYSeries(name);
        data.add(ret);
        return ret;
    }

    public void addSeries(XYSeries series) {
        data.add(series);
    }

    public void addSeries(Collection<XYSeries> series) {
        data.addAll(series);
    }

    public @Nullable XYSeries getSeries(String name) {
        for (XYSeries ret : data) {
            if (ret.name.equals(name)) {
                return ret;
            }
        }
        return null;
    }

    public XYSeries getOrNewSeries(String name) {
        for (XYSeries ret : data) {
            if (ret.name.equals(name)) {
                return ret;
            }
        }

        var ret = new XYSeries(name);
        data.add(ret);
        return ret;
    }

    public @Nullable XYSeries removeSeries(String name) {
        for (int i = 0, size = data.size(); i < size; i++) {
            var ret = data.get(i);
            if (ret.name.equals(name)) {
                data.remove(i);
                return ret;
            }
        }
        return null;
    }

    public void retainSeries(Collection<String> name) {
        data.removeIf(it -> !name.contains(it.name));
    }

    public void clearSeries() {
        data.clear();
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
