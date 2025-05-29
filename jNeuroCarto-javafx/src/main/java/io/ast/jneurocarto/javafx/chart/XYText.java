package io.ast.jneurocarto.javafx.chart;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class XYText extends XYSeries {

    protected @Nullable Font font;
    protected @Nullable Color color = null;

    public @Nullable Font font() {
        return font;
    }

    public void font(Font font) {
        this.font = font;
    }

    public @Nullable Color color() {
        return color;
    }

    public void color(@Nullable Color color) {
        this.color = color;
    }

    @Override
    public int transform(Affine aff, double[][] p) {
        var data = this.data;

        var ret = 0;
        for (int i = 0, length = data.size(); i < length; i++) {
            var xy = data.get(i);
            if (xy.external instanceof String) {
                var j = ret++;
                var q = aff.transform(xy.x, xy.y);

                p[0][j] = q.getX();
                p[1][j] = q.getY();
                p[2][j] = i;
            }
        }

        return ret;
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (color == null) return;

        gc.save();
        try {
            gc.setTransform(InteractionXYPainter.IDENTIFY);
            gc.setGlobalAlpha(alpha);
            gc.setFill(color);
            if (font != null) gc.setFont(font);

            for (int i = 0; i < length; i++) {
                var x = p[0][i];
                var y = p[1][i];
                var j = (int) p[2][i];
                var o = data.get(j).external;
                if (o instanceof String text) {
                    gc.fillText(text, x, y);
                }
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

    public static class Builder extends XYSeries.Builder<XYText, Builder> {
        public Builder(XYText graphics) {
            super(graphics);
        }

        public Builder font(Font font) {
            graphics.font(font);
            return this;
        }

        public Builder color(@Nullable Color color) {
            graphics.color(color);
            return this;
        }

        public Builder addText(String text, double x, double y) {
            graphics.addData(new XY(x, y, text));
            return this;
        }
    }
}
