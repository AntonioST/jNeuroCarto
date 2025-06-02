package io.ast.jneurocarto.javafx.chart.data;

import java.util.function.ToDoubleFunction;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.blueprint.MinMaxInt;

@NullMarked
public class XYMatrix extends XYSeries {

    protected double x = 0;
    protected double y = 0;
    protected double w = 1;
    protected double h = 1;

    private int numberOfData;
    private @Nullable MinMaxInt xr;
    private @Nullable MinMaxInt yr;
    private int nx = -1;
    private int ny = -1;

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

    public double w() {
        return w;
    }

    public void w(double w) {
        if (w < 0) x += w;
        this.w = Math.abs(w);
    }

    public double h() {
        return h;
    }

    public void h(double h) {
        if (h < 0) y += h;
        this.h = Math.abs(h);
    }

    public void extent(double x, double y, double w, double h) {
        this.x = w < 0 ? x + w : x;
        this.y = h < 0 ? y + h : y;
        this.w = Math.abs(w);
        this.h = Math.abs(h);
    }

    public void extent(double x, double y, int nx, double dw, int ny, double dh) {
        if (nx <= 0) throw new IllegalArgumentException();
        if (ny <= 0) throw new IllegalArgumentException();
        extent(x, y, nx * dw, ny * dh);
        this.nx = nx;
        this.ny = ny;
    }

    public int nx() {
        return nx;
    }

    public void nx(int nx) {
        this.nx = nx;
    }

    public int ny() {
        return ny;
    }

    public void ny(int ny) {
        this.ny = ny;
    }

    /**
     * number of data points.
     *
     * @return always 0. It has no points need to be transformed.
     */
    @Override
    public int points() {
        return 0;
    }

    public void addData(double[] data) {
        addData(data, 1);
    }

    public void addData(double[] data, int row) {
        addData(data, row, true);
    }

    /**
     * @param data a 2d-flatten double array.
     * @param row  number of row
     * @param flip flip y direction. If {@code true}, the first row has largest y pos.
     */
    public void addData(double[] data, int row, boolean flip) {
        if (row <= 0) throw new IllegalArgumentException();

        int column = (int) (Math.ceil((double) data.length / row));
        for (int r = 0; r < row; r++) {
            var y = flip ? row - r - 1 : r;

            for (int c = 0; c < column; c++) {
                var i = r * column + c;
                if (i < data.length) {
                    var v = data[i];
                    if (!Double.isNaN(v)) {
                        var x = c;

                        addData(x, y, v);
                    }
                }
            }
        }

        nx = column;
        ny = row;
    }

    public void addData(double[][] data, boolean flip) {
        ny = data.length;
        nx = 0;

        for (int r = 0, nr = data.length; r < nr; r++) {
            var y = flip ? nr - r - 1 : r;

            var row = data[r];
            nx = Math.max(nx, row.length);

            for (int c = 0, nc = row.length; c < nc; c++) {
                var v = row[c];
                if (!Double.isNaN(v)) {
                    var x = c;
                    addData(x, y, v);
                }
            }
        }
    }

    @Override
    public int transform(Affine aff, double[][] p) {
        var data = this.data;
        var length = data.size();

        if (length > 0 && length != numberOfData) {
            numberOfData = length;
            xr = minmax(XY::x);
            if (xr == null) return 0;

            yr = minmax(XY::y);
            assert yr != null;
        }

        return 0;
    }

    private @Nullable MinMaxInt minmax(ToDoubleFunction<XY> f) {
        return data.stream().mapToDouble(f)
          .mapToInt(x -> (int) x)
          .boxed()
          .gather(MinMaxInt.intMinmax())
          .findFirst().orElse(null);
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (colormap == null) return;

        var cmap = colormap;

        assert xr != null;
        assert yr != null;

        var x0 = xr.min();
        var y0 = yr.min();
        int nx = this.nx > 0 ? this.nx : xr.range() + 1;
        int ny = this.ny > 0 ? this.ny : yr.range() + 1;

        var dw = w / nx;
        var dh = h / ny;

        var aff = gc.getTransform();
        Affine inv;
        try {
            inv = aff.createInverse();
        } catch (NonInvertibleTransformException e) {
            inv = null;
        }

        if (inv != null) {
            // fix 1px gaps
            var q = inv.deltaTransform(1, 1);
            dw += Math.abs(q.getX());
            dh += Math.abs(q.getY());
        }

        gc.save();
        try {
            gc.setGlobalAlpha(alpha);
            gc.setEffect(effect);

            for (var xy : data) {
                if (Double.isNaN(xy.x + xy.y + xy.v)) {
                    continue;
                }

                var px = x + w * ((int) xy.x - x0) / nx;
                var py = y + h * ((int) xy.y - y0) / ny;

                gc.setFill(cmap.apply(xy.v));
                gc.fillRect(px, py, dw, dh);
            }

        } finally {
            gc.restore();
        }
    }

    /*=========*
     * builder *
     *=========*/

    public Builder builder() {
        return new Builder(this);
    }

    public static class Builder extends XYSeries.Builder<XYMatrix, Builder> {
        public Builder(XYMatrix graphics) {
            super(graphics);
        }

        public Builder x(double x) {
            graphics.x(x);
            return this;
        }

        public Builder y(double y) {
            graphics.y(y);
            return this;
        }

        public Builder w(double w) {
            graphics.w(w);
            return this;
        }

        public Builder h(double h) {
            graphics.h(h);
            return this;
        }

        public Builder extent(double x, double y, double w, double h) {
            graphics.extent(x, y, w, h);
            return this;
        }

        public Builder extent(double x, double y, int nx, double dw, int ny, double dh) {
            graphics.extent(x, y, nx, dw, ny, dh);
            return this;
        }

        public Builder nx(int nx) {
            graphics.nx(nx);
            return this;
        }

        public Builder ny(int ny) {
            graphics.ny(ny);
            return this;
        }

        public Builder addData(int x, int y) {
            graphics.addData(x, y);
            return this;
        }

        public Builder addData(int x, int y, double v) {
            graphics.addData(x, y, v);
            return this;
        }

        public Builder addData(Point2D p) {
            return addData((int) p.getX(), (int) p.getY());
        }

        public Builder addData(Point2D p, double v) {
            return addData((int) p.getX(), (int) p.getY(), v);
        }

        public Builder clearData() {
            graphics.clearData();
            return this;
        }
    }
}
