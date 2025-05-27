package io.ast.jneurocarto.javafx.chart;

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


    /**
     * {@inheritDoc}
     * <br/>
     * It is used when {@link #fill()} set to {@link Color#TRANSPARENT}.
     *
     * @param colormap
     */
    public void colormap(Colormap colormap) {
        super.colormap(colormap);
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (fill == null && edge == null) return;

        gc.save();
        try {
            gc.setTransform(InteractionXYPainter.IDENTIFY);
            gc.setGlobalAlpha(alpha);
            gc.setLineWidth(ew);

            if (normalize != null && colormap != null && fill == Color.TRANSPARENT) {
                paintMarkers(gc, p, offset, length, colormap, normalize);
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
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                gc.fillRect(x - dx, y - dy, w, h);
            }
        }
    }

    private void paintMarkers(GraphicsContext gc, double[][] p, int offset, int length, Colormap cmap, Normalize norm) {
        assert fill != null;

        var dx = w / 2;
        var dy = h / 2;

        for (int i = 0; i < length; i++) {
            var x = p[0][i + offset];
            var y = p[1][i + offset];
            var v = p[2][i + offset];
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                gc.setFill(cmap.get(norm, v));
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
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                gc.strokeRect(x - dx, y - dy, w, h);
            }
        }
    }
}
