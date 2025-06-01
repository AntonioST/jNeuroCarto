package io.ast.jneurocarto.javafx.chart;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class XYPath extends XYSeries.XYColormapSeries {

    protected double lw = 1;
    protected @Nullable Color line = null;
    protected @Nullable Color fill = null;

    public double linewidth() {
        return lw;
    }

    public void linewidth(double lw) {
        this.lw = lw;
    }

    /**
     * {@return fill color inside lines}
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

    public void line(@Nullable Color line) {
        this.line = line;
    }

    /**
     * set colormap.
     * <br/>
     * It is used when {@link #line()} set to {@link Color#TRANSPARENT}.
     *
     * @param colormap
     */
    public void colormap(Colormap colormap) {
        this.colormap = colormap;
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (fill == null && line == null && length == 0) return;

        gc.save();
        try {
            gc.setTransform(InteractionXYPainter.IDENTIFY);
            gc.setGlobalAlpha(alpha);
            gc.setEffect(effect);
            gc.setLineWidth(lw);

            if (fill != null) {
                paintFill(gc, p, offset, length);
            }

            if (colormap != null && line == Color.TRANSPARENT) {
                paintLine(gc, p, offset, length, colormap);
            } else if (line != null) {
                paintLine(gc, p, offset, length);
            }

        } finally {
            gc.restore();
        }
    }

    private void paintLine(GraphicsContext gc, double[][] p, int offset, int length) {
        assert line != null;

        gc.setStroke(line);

        var counter = 0;

        for (int i = 0; i < length; i++) {
            var j = i + offset;

            var x = p[0][j];
            var y = p[1][j];
            if (Double.isNaN(x) || Double.isNaN(y)) {
                counter = 0;
                gc.stroke();
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
            gc.stroke();
        }
    }

    private void paintLine(GraphicsContext gc, double[][] p, int offset, int length, Colormap cmap) {
        assert line == Color.TRANSPARENT;

        var x1 = p[0][offset];
        var y1 = p[1][offset];
        var v1 = p[2][offset];

        for (int i = 1; i < length; i++) {
            var x2 = p[0][i + offset];
            var y2 = p[1][i + offset];
            var v2 = p[2][i + offset];
            if (!Double.isNaN(x1 + y1 + x2 + y2 + v1 + v2)) {
                gc.setStroke(cmap.gradient(x1, y1, x2, y2, v1, v2));
                gc.strokeLine(x1, y1, x2, y2);
            }
            x1 = x2;
            y1 = y2;
            v1 = v2;
        }
    }

    private void paintFill(GraphicsContext gc, double[][] p, int offset, int length) {
        assert fill != null;

        gc.setFill(fill);

        var counter = 0;

        for (int i = 0; i < length; i++) {
            var j = i + offset;

            var x = p[0][j];
            var y = p[1][j];
            if (Double.isNaN(x) || Double.isNaN(y)) {
                counter = 0;
                gc.fill();
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
            gc.fill();
        }
    }

    /*=========*
     * builder *
     *=========*/

    public Builder builder() {
        return new Builder(this);
    }

    public static class Builder extends XYSeries.XYColormapSeriesBuilder<XYPath, Builder> {
        public Builder(XYPath graphics) {
            super(graphics);
        }

        public Builder linewidth(double lw) {
            graphics.linewidth(lw);
            return this;
        }

        public Builder fill(String fill) {
            return fill(Color.valueOf(fill));
        }

        public Builder fill(@Nullable Color fill) {
            graphics.fill(fill);
            return this;
        }

        public Builder line(String line) {
            return line(Color.valueOf(line));
        }

        public Builder line(@Nullable Color line) {
            graphics.line(line);
            return this;
        }

        public Builder addPoint(double x, double y) {
            graphics.addData(x, y);
            return this;
        }

        public Builder addPoint(double x, double y, double v) {
            graphics.addData(x, y, v);
            return this;
        }

        public Builder addPoint(Point2D p) {
            graphics.addData(p);
            return this;
        }

        public Builder addPoint(Point2D p, double v) {
            graphics.addData(p, v);
            return this;
        }

        public Builder addPoints(double x, double dx, double[] y) {
            return addPoints(x, dx, 0, y);
        }

        public Builder addPoints(double x, double dx, double y, double[] dy) {
            for (int i = 0, length = dy.length; i < length; i++) {
                graphics.addData(x + dx * i, y + dy[i]);
            }
            return this;
        }

        public Builder addPoints(double[] x, double y, double dy) {
            return addPoints(0, x, y, dy);
        }

        public Builder addPoints(double x, double[] dx, double y, double dy) {
            for (int i = 0, length = dx.length; i < length; i++) {
                graphics.addData(x + dx[i], y + dy * i);
            }
            return this;
        }

        public Builder addPoints(double[] x, double[] y) {
            return addPoints(0, x, 0, y);
        }

        public Builder addPoints(double x, double[] dx, double y, double[] dy) {
            int length = dx.length;
            if (length != dy.length) throw new IllegalArgumentException();
            for (int i = 0; i < length; i++) {
                graphics.addData(x + dx[i], y + dy[i]);
            }
            return this;
        }
    }
}
