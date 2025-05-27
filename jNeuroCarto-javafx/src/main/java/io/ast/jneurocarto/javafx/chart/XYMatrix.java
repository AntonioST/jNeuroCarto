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
        var value = this.value;
        var length = data.size();

        var xr = minmax(XY::x);
        if (xr == null) return 0;

        var yr = minmax(XY::y);
        assert yr != null;

        var x0 = xr.min();
        var y0 = yr.min();
        int nx = xr.range() + 1;
        int ny = yr.range() + 1;

        for (int i = 0; i < length; i++) {
            var xy = data.get(i);
            var x = this.x + w * ((int) xy.x - x0) / nx;
            var y = this.y + h * ((int) xy.y - y0) / ny;
            var q = aff.transform(x, y);
            p[0][i] = q.getX();
            p[1][i] = q.getY();
            p[2][i] = xy.v;
            p[3][i] = value == null ? 0 : value.applyAsDouble(xy.external);
        }

        var q = aff.deltaTransform(w, h);
        dw = q.getX() / nx;
        dh = q.getY() / ny;

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
        if (colormap == null || normalize == null) return;

        gc.save();
        try {
            gc.setTransform(InteractionXYPainter.IDENTIFY);
            gc.setGlobalAlpha(alpha);
            paintMatrix(gc, p, offset, length);
        } finally {
            gc.restore();
        }
    }

    private void paintMatrix(GraphicsContext gc, double[][] p, int offset, int length) {
        assert colormap != null;
        assert normalize != null;

        var dx = dw / 2;
        var dy = dh / 2;
        var cmap = colormap;
        var norm = normalize;

        for (int i = 0; i < length; i++) {
            var x = p[0][i + offset];
            var y = p[1][i + offset];
            var v = p[2][i + offset];
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                gc.setFill(cmap.get(norm, v));
                gc.fillRect(x - dx, y - dy, dw, dh);
            }
        }
    }
}
