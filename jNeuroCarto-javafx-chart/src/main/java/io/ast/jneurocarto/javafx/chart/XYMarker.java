package io.ast.jneurocarto.javafx.chart;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class XYMarker extends XYSeries {

    protected double w = 1;
    protected double h = 1;
    protected double ew = 1;
    protected @Nullable Color edge = null;
    protected @Nullable Color fill = null;

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

    public double edgewidth() {
        return ew;
    }

    public void edgewidth(double ew) {
        this.ew = ew;
    }

    /**
     * {@return border color of markers}
     */
    public @Nullable Color edge() {
        return edge;
    }

    public void edge(@Nullable Color border) {
        this.edge = border;
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

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (fill == null && edge == null) return;

        gc.save();
        try {
            gc.setTransform(InteractionXYPainter.IDENTIFY);
            gc.setGlobalAlpha(alpha);
            gc.setEffect(effect);
            gc.setLineWidth(ew);

            if (colormap != null) {
                paintMarkers(gc, p, offset, length, colormap);
            } else if (fill != null) {
                paintMarkers(gc, p, offset, length);
            }

            if (edge != null) {
                paintMarkersEdge(gc, p, offset, length);
            }
        } finally {
            gc.restore();
        }
    }

    private void paintMarkers(GraphicsContext gc, double[][] p, int offset, int length) {
        assert fill != null;

        var dx = w / 2;
        var dy = h / 2;

        gc.setFill(fill);
        for (int i = 0; i < length; i++) {
            var x = p[0][i + offset];
            var y = p[1][i + offset];
            if (!Double.isNaN(x + y)) {
                gc.fillRect(x - dx, y - dy, w, h);
            }
        }
    }

    private void paintMarkers(GraphicsContext gc, double[][] p, int offset, int length, Colormap cmap) {
        assert fill != null;

        var dx = w / 2;
        var dy = h / 2;

        for (int i = 0; i < length; i++) {
            var x = p[0][i + offset];
            var y = p[1][i + offset];
            var v = p[2][i + offset];
            if (!Double.isNaN(x + y + v)) {
                gc.setFill(cmap.apply(v));
                gc.fillRect(x - dx, y - dy, w, h);
            }
        }
    }

    private void paintMarkersEdge(GraphicsContext gc, double[][] p, int offset, int length) {
        assert edge != null;

        var dx = w / 2;
        var dy = h / 2;

        for (int i = 0; i < length; i++) {
            var x = p[0][i + offset];
            var y = p[1][i + offset];
            if (!Double.isNaN(x + y)) {
                gc.strokeRect(x - dx, y - dy, w, h);
            }
        }
    }

    /*=========*
     * builder *
     *=========*/

    public Builder builder() {
        return new Builder(this);
    }

    public static class Builder extends XYSeries.Builder<XYMarker, Builder> {
        public Builder(XYMarker graphics) {
            super(graphics);
        }

        public Builder w(double w) {
            graphics.w(w);
            return this;
        }

        public Builder h(double h) {
            graphics.h(h);
            return this;
        }

        public Builder wh(double w, double h) {
            graphics.w(w);
            graphics.h(h);
            return this;
        }

        public Builder edgewidth(double lw) {
            graphics.edgewidth(lw);
            return this;
        }

        public Builder edge(String line) {
            return edge(Color.valueOf(line));
        }

        public Builder edge(@Nullable Color line) {
            graphics.edge(line);
            return this;
        }

        public Builder fill(String fill) {
            return fill(Color.valueOf(fill));
        }

        public Builder fill(@Nullable Color fill) {
            graphics.fill(fill);
            return this;
        }

        public Builder addMarker(double x, double y) {
            graphics.addData(x, y);
            return this;
        }

        public Builder addMarker(double x, double y, double v) {
            graphics.addData(x, y, v);
            return this;
        }

        public Builder addMarker(Point2D p) {
            graphics.addData(p);
            return this;
        }

        public Builder addMarker(Point2D p, double v) {
            graphics.addData(p, v);
            return this;
        }

        public Builder clearMarkers() {
            graphics.clearData();
            return this;
        }
    }
}
