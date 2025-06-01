package io.ast.jneurocarto.javafx.chart;


import java.util.IdentityHashMap;
import java.util.List;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Effect;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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
    protected @Nullable Effect textEffect = null;
    private @Nullable Bounds bounds;
    private double baselineOffset;

    public @Nullable Font font() {
        return font;
    }

    public void font(Font font) {
        this.font = font;
        bounds = null;

        updateBaselineOffset();
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
        updateBaselineOffset();
    }

    private void updateBaselineOffset() {
        if (baseline == VPos.BASELINE) {
            var t = new Text("");
            t.setFont(font);
            baselineOffset = t.getBaselineOffset();
        }
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

    public @Nullable Effect textEffect() {
        return textEffect;
    }

    public void textEffect(@Nullable Effect effect) {
        this.textEffect = effect;
    }

    /*==============*
     * special data *
     *==============*/

    private record Annotation(XY data) {
    }

    /*===========*
     * selecting *
     *===========*/

    private @Nullable BoundingBox boundOf(XY xy) {
        double x = xy.x;
        double y = xy.y;

        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        return switch (xy.external) {
            case String text -> boundOfText(text, x, y);
            case Annotation(var prev) -> boundOfAnnotation(xy, prev);
            case null, default -> null;
        };
    }

    private BoundingBox boundOfAnnotation(XY p1, XY p2) {
        var x = Math.min(p1.x, p2.x);
        var y = Math.min(p1.y, p2.y);
        var w = Math.abs(p1.x - p2.x);
        var h = Math.abs(p1.y - p2.y);
        return new BoundingBox(x, y, w, h);
    }

    private BoundingBox boundOfText(String text, double x, double y) {
        if (bounds == null) {
            var temp = new Text(text);
            temp.setFont(font);
            bounds = temp.getLayoutBounds();
        }

        var width = bounds.getWidth();
        var height = bounds.getHeight();

        var dx = switch (align) {
            case CENTER -> -width / 2;
            case RIGHT -> -width;
            case null, default -> 0;
        };

        var dy = switch (baseline) {
            case TOP -> 0;
            case CENTER -> -height / 2;
            case BOTTOM -> -height;
            case BASELINE -> -baselineOffset;
            case null -> -baselineOffset;
        };

        return new BoundingBox(x + dx, y + dy, width, height);
    }

    @Override
    public @Nullable XY touch(Point2D p) {
        return data.stream().filter(xy -> {
            var b = boundOf(xy);
            return b != null && b.contains(p);
        }).findFirst().orElse(null);
    }

    @Override
    public @Nullable XY touch(Point2D p, double radius) {
        return touch(p);
    }

    @Override
    public List<XY> touch(Bounds bounds) {
        return data.stream().filter(xy -> {
            var b = boundOf(xy);
            return b != null && bounds.contains(b);
        }).toList();
    }

    /*================*
     * Transformation *
     *================*/

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

    /*==========*
     * plotting *
     *==========*/

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
                            gc.setEffect(textEffect);
                            gc.fillText(text, x, y);
                        }
                    } else {
                        var x1 = p[0][j];
                        var y1 = p[1][j];
                        gc.setEffect(effect);
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

        public Builder textEffect(@Nullable Effect effect) {
            graphics.textEffect(effect);
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
