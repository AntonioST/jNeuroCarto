package io.ast.jneurocarto.javafx.chart;


import java.util.IdentityHashMap;

import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class XYText extends XYSeries {

    protected @Nullable Font font;
    protected @Nullable TextAlignment align;
    protected @Nullable VPos baseline;
    protected @Nullable Color color = null;
    protected @Nullable Color line = null;

    public @Nullable Font font() {
        return font;
    }

    public void font(Font font) {
        this.font = font;
    }

    public @Nullable TextAlignment align() {
        return align;
    }

    public void align(@Nullable TextAlignment align) {
        this.align = align;
    }

    public @Nullable VPos baseline() {
        return baseline;
    }

    public void baseline(@Nullable VPos baseline) {
        this.baseline = baseline;
    }

    public @Nullable Color color() {
        return color;
    }

    public void color(@Nullable Color color) {
        this.color = color;
    }

    public @Nullable Color line() {
        return line;
    }

    public void line(@Nullable Color color) {
        this.line = color;
    }

    private record Annotation(XY data) {
    }

    /**
     * {@inheritDoc}
     * <br/>
     * The third columns is used as an index to make an annotation line point to the index-th {@link XY}.
     *
     * @param aff {@link GraphicsContext}'s affine transformation.
     * @param p   {@code double[4][row]} array that store the transformed data.
     * @return
     */
    @Override
    public int transform(Affine aff, double[][] p) {
        var data = this.data;

        var length = data.size();
        var index = new IdentityHashMap<XY, Integer>(length);

        var linespace = (font == null ? 12 : font.getSize()) * 1.2;

        for (int i = 0; i < length; i++) {
            var xy = data.get(i);

            switch (xy.external) {
            case String s -> {
                var q = aff.transform(xy.x, xy.y);

                p[0][i] = q.getX();
                p[1][i] = q.getY();
                p[2][i] = -1;
                index.put(xy, i);
            }
            case Annotation(var prev) -> {
                var k = index.getOrDefault(prev, -1);
                if (k >= 0) {
                    var q = aff.transform(xy.x, xy.y);

                    p[0][i] = q.getX();
                    p[1][i] = q.getY();
                    p[2][i] = k;
                } else {
                    p[0][i] = Double.NaN;
//                    p[1][i] = Double.NaN;
//                    p[2][i] = 0;
                }
            }
            case null, default -> {
                p[0][i] = Double.NaN;
//                p[1][i] = Double.NaN;
//                p[2][i] = 0;
            }

            }
        }

        return length;
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (color == null) return;

        gc.save();
        try {
            gc.setTransform(InteractionXYPainter.IDENTIFY);
            gc.setGlobalAlpha(alpha);
            gc.setFill(color);
            gc.setStroke(line);
            gc.setFont(font);
            gc.setTextAlign(align);
            gc.setTextBaseline(baseline);

            for (int i = 0; i < length; i++) {
                var x = p[0][i];
                var y = p[1][i];

                if (!Double.isNaN(x + y)) {
                    var j = (int) p[2][i];
                    if (j < 0) {
                        var o = data.get(i).external;
                        if (o instanceof String text) {
                            gc.fillText(text, x, y);
                        }
                    } else {
                        var x1 = p[0][j];
                        var y1 = p[1][j];
                        gc.strokeLine(x, y, x1, y1);
                    }
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

        public Builder align(@Nullable TextAlignment align) {
            graphics.align(align);
            return this;
        }

        public Builder baseline(@Nullable VPos baseline) {
            graphics.baseline(baseline);
            return this;
        }

        public Builder color(@Nullable Color color) {
            graphics.color(color);
            return this;
        }

        public Builder line(@Nullable Color color) {
            graphics.line(color);
            return this;
        }

        public Builder addText(String text, double x, double y) {
            graphics.addData(new XY(x, y, text));
            return this;
        }

        public Builder addText(String text, Point2D p) {
            graphics.addData(new XY(p, text));
            return this;
        }

        /**
         * Create a text with an annotation line.
         *
         * @param text text
         * @param x    text x position
         * @param y    text y position
         * @param px   x position of the annotation line point to
         * @param py   y position of the annotation line point to
         * @return
         */
        public Builder addText(String text, double x, double y, double px, double py) {
            var data = new XY(x, y, text);
            graphics.addData(data);
            graphics.addData(new XY(px, py, new Annotation(data)));
            return this;
        }

        /**
         * Create a text with an annotation line.
         *
         * @param text text
         * @param p    text position
         * @param a    position of the annotation line point to
         * @return
         */
        public Builder addText(String text, Point2D p, Point2D a) {
            var data = new XY(p, text);
            graphics.addData(data);
            graphics.addData(new XY(a, new Annotation(data)));
            return this;
        }
    }
}
