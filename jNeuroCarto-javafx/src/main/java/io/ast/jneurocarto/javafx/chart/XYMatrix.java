package io.ast.jneurocarto.javafx.chart;

import java.util.function.ToDoubleFunction;

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
        this.w = w;
    }

    public double h() {
        return h;
    }

    public void h(double h) {
        this.h = h;
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
    }

    public void addData(double[][] data, boolean flip) {
        for (int r = 0, nr = data.length; r < nr; r++) {
            var y = flip ? nr - r - 1 : r;

            var row = data[r];
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
          .gather(MinMaxInt.minmax())
          .findFirst().orElse(null);
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (colormap == null) return;

        var cmap = colormap;
        var norm = normalize;
        if (norm == null) norm = renormalize();

        assert xr != null;
        assert yr != null;

        var x0 = xr.min();
        var y0 = yr.min();
        int nx = xr.range() + 1;
        int ny = yr.range() + 1;

        var dw = w / nx;
        var dh = h / ny;

        try {
            // fix 1px gaps
            var q = gc.getTransform().inverseDeltaTransform(1, 1);
            dw += q.getX();
            dh += q.getY();
        } catch (NonInvertibleTransformException e) {
            throw new RuntimeException(e);
        }

        gc.save();
        try {
            gc.setGlobalAlpha(alpha);

            for (var xy : data) {
                if (Double.isNaN(xy.x) || Double.isNaN(xy.y)) {
                    continue;
                }

                var px = x + w * ((int) xy.x - x0) / nx;
                var py = y + h * ((int) xy.y - y0) / ny;

                gc.setFill(cmap.get(norm, xy.v));
                gc.fillRect(px, py, dw, dh);
            }

        } finally {
            gc.restore();
        }
    }
}
