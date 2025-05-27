package io.ast.jneurocarto.javafx.chart;

import java.util.function.ToDoubleFunction;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.blueprint.MinMaxInt;

@NullMarked
public class XYMatrix extends XYSeries {

    protected double x = 0;
    protected double y = 0;
    protected double w = 1;
    protected double h = 1;
    private double dw;
    private double dh;

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

    @Override
    public int transform(Affine aff, double[][] p) {
        var data = this.data;
        var length = data.size();
        if (length == 0) return 0;

        if (length != numberOfData) {
            numberOfData = length;
            xr = minmax(XY::x);
            if (xr == null) return 0;

            yr = minmax(XY::y);
            assert yr != null;
        }

        assert xr != null;
        assert yr != null;

        var x0 = xr.min();
        var y0 = yr.min();
        int nx = xr.range() + 1;
        int ny = yr.range() + 1;

        {
            var q = aff.deltaTransform(w, h);
            dw = q.getX() / nx;
            dh = q.getY() / ny;
        }

        var dx = dw >= 0 ? 0 : dw;
        var dy = dh >= 0 ? 0 : dh;

        for (int i = 0; i < length; i++) {
            var xy = data.get(i);
            if (Double.isNaN(xy.x) || Double.isNaN(xy.y)) {
                length--;
                i--;
                continue;
            }

            var px = x + w * ((int) xy.x - x0) / nx;
            var py = y + h * ((int) xy.y - y0) / ny;
            var q = aff.transform(px, py);

            p[0][i] = q.getX() + dx;
            p[1][i] = q.getY() + dy;
            p[2][i] = xy.v;
        }

        return length;
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

        var norm = normalize;
        if (norm == null) norm = renormalize();

        gc.save();
        try {
            gc.setTransform(InteractionXYPainter.IDENTIFY);
            gc.setGlobalAlpha(alpha);
            paintMatrix(gc, p, offset, length, colormap, norm);
        } finally {
            gc.restore();
        }
    }

    private void paintMatrix(GraphicsContext gc, double[][] p, int offset, int length, Colormap cmap, Normalize norm) {
        var dw = Math.abs(this.dw) + 1; // 1 for gaps
        var dh = Math.abs(this.dh) + 1;

        for (int i = 0; i < length; i++) {
            var x = p[0][i + offset];
            var y = p[1][i + offset];
            var v = p[2][i + offset];
            gc.setFill(cmap.get(norm, v));
            gc.fillRect(x, y, dw, dh);
        }
    }
}
