package io.ast.jneurocarto.javafx.chart.data;


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
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Text graphics.
///
/// The [XY] carried by this should follow the rule of:
/// the [XY.external][XY#external] object should be either
/// 1.  [String]. The [XY] is a text carried that display the text, or
/// 2.  [XY]. The [XY] is an annotation line point from carried [XY] (usually a text carrier) to itself.
/// 3.  Otherwise, ignored.
///
/// ### Color
///
/// For a text, if [colormap][#colormap] is not set, use [color][#color]. Otherwise, the color is based on
/// the [XY.v][XY#v] in the [colormap][#colormap].
/// If [XY.v][XY#v] is [Double.NaN][Double#NaN], then always use [color][#color].
///
/// For an annotation, its color is [line][#line].
///
/// To hide text, set [XY.v][XY#x] or [XY.v][XY#y] to [Double.NaN][Double#NaN].
///
/// ### Effect
@NullMarked
public class XYText extends XYSeries {

    protected @Nullable Font font;
    protected @Nullable TextAlignment align;
    protected @Nullable VPos baseline;
    protected @Nullable Color color = null;
    protected @Nullable Color line = null;
    protected @Nullable Effect textEffect = null;
    protected boolean showAnchorPoint;
    protected boolean showTextBounds;

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

    public @Nullable Effect textEffect() {
        return textEffect;
    }

    public void textEffect(@Nullable Effect effect) {
        this.textEffect = effect;
    }

    public boolean showAnchorPoint() {
        return showAnchorPoint;
    }

    public void showAnchorPoint(boolean showAnchorPoint) {
        this.showAnchorPoint = showAnchorPoint;
    }

    public boolean showTextBounds() {
        return showTextBounds;
    }

    public void showTextBounds(boolean showTextBounds) {
        this.showTextBounds = showTextBounds;
    }

    /*===========*
     * selecting *
     *===========*/

    @Override
    public @Nullable XY touch(Point2D p) {
        Affine aff;
        try {
            aff = cachedAffine == null ? null : cachedAffine.createInverse();
        } catch (NonInvertibleTransformException e) {
            aff = null;
        }
        var finalAff = aff;

        return data.stream().filter(xy -> {
            var ext = xy.external;
            if (finalAff != null && ext instanceof String text) {
                return boundOfText(finalAff, text, xy.x, xy.y).contains(p);
            } else if (ext instanceof XY p2) {
                return touchLineSeg(xy, p2, p, 1);
            } else {
                return false;
            }
        }).findFirst().orElse(null);
    }

    @Override
    public @Nullable XY touch(Point2D p, double radius) {
        return touch(p);
    }

    @Override
    public List<XY> touch(Bounds bounds) {
        if (cachedAffine == null) return List.of();
        Affine aff;
        try {
            aff = cachedAffine.createInverse();
        } catch (NonInvertibleTransformException e) {
            return List.of();
        }

        return data.stream().filter(xy -> {
            var b = boundOf(aff, xy);
            return (b != null && bounds.contains(b)) || (bounds.contains(xy.x, xy.y));
        }).toList();
    }

    /**
     * Get boundary for {@code xy}.
     *
     * @param aff a transformation from canvas to chart.
     * @param xy  a {@link XY} data.
     * @return a boundary in chart coordinate.
     */
    private @Nullable BoundingBox boundOf(Affine aff, XY xy) {
        double x = xy.x;
        double y = xy.y;

        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        return switch (xy.external) {
            case String text -> boundOfText(aff, text, x, y);
            case XY prev -> boundOfAnnotation(xy, prev);
            case null, default -> null;
        };
    }

    /**
     * Get annotation boundary.
     *
     * @param p1 start point in chart coordinate.
     * @param p2 end point in chart coordinate.
     * @return a annotation boundary in chart coordinate.
     */
    private BoundingBox boundOfAnnotation(XY p1, XY p2) {
        var x = Math.min(p1.x, p2.x);
        var y = Math.min(p1.y, p2.y);
        var w = Math.abs(p1.x - p2.x);
        var h = Math.abs(p1.y - p2.y);
        return new BoundingBox(x, y, w, h);
    }

    /**
     * Get text boundary.
     *
     * @param aff  a transformation from canvas to chart.
     *             If it is {@code null}, change the {@code x}, {@code y} and the return in canvas coordinate.
     * @param text text content
     * @param x    x position in chart coordinate.
     * @param y    y position in chart coordinate.
     * @return a text boundary in chart coordinate.
     */
    private BoundingBox boundOfText(@Nullable Affine aff, String text, double x, double y) {
        var temp = new Text(text);
        temp.setFont(font);

        var bounds = temp.getLayoutBounds();
        var width = bounds.getWidth();
        var height = bounds.getHeight();

        if (aff != null) {
            var delta = aff.deltaTransform(width, height);
            width = delta.getX();
            height = delta.getY();
        }

        var dx = switch (align) {
            case CENTER -> -width / 2;
            case RIGHT -> -width;
            case null, default -> 0;
        };

        var dy = switch (baseline == null ? VPos.BASELINE : baseline) {
            case TOP -> 0;
            case CENTER -> -height / 2;
            case BOTTOM -> -height;
            case BASELINE -> {
                var baseline = temp.getBaselineOffset();
                if (aff != null) {
                    var delta = aff.deltaTransform(0, baseline);
                    baseline = delta.getY();
                }
                yield -baseline;
            }
        };

        if (width < 0) {
            dx += width;
            width = -width;
        }

        if (height < 0) {
            dy += height;
            height = -height;
        }

        return new BoundingBox(x + dx, y + dy, width, height);
    }

    private boolean touchLineSeg(XY p1, XY p2, Point2D p, double radius) {
        // modified based on GPT
        var x0 = p.getX();
        var y0 = p.getY();
        var x1 = p1.x();
        var y1 = p1.y();
        var x2 = p2.x();
        var y2 = p2.y();

        var dx = x2 - x1;
        var dy = y2 - y1;

        // Handle the case when the segment is a point
        if (dx == 0 && dy == 0) {
            return p.distance(x1, y1) < radius;
        }

        // Project point p onto the line segment
        double t = ((x0 - x1) * dx + (y0 - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t)); // Clamp t to [0, 1] to stay within segment

        // Closest point on the segment to p
        var x = x1 + t * dx;
        var y = y1 + t * dy;

        // Compute distance to the closest point
        return p.distance(x, y) < radius;
    }

    /*================*
     * Transformation *
     *================*/

    /**
     * a cached transformation from chart to canvas.
     */
    private @Nullable Affine cachedAffine;

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
        cachedAffine = aff;
        var data = this.data;

        var length = data.size();
        var index = new IdentityHashMap<XY, Integer>(length);

        for (int i = 0; i < length; i++) {
            var xy = data.get(i);

            switch (xy.external) {
            case String s -> {
                if (!Double.isNaN(xy.x + xy.v)) {
                    var q = aff.transform(xy.x, xy.y);

                    p[0][i] = q.getX();
                    p[1][i] = q.getY();
                    p[2][i] = -1;
                    index.put(xy, i);
                } else {
                    p[0][i] = Double.NaN;
                }
            }
            case XY prev -> {
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
        var cmap = colormap;

        gc.save();
        try {
            gc.setTransform(IDENTIFY);
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
                        var xy = data.get(i);
                        var o = xy.external;
                        if (o instanceof String text) {
                            if (cmap != null && !Double.isNaN(xy.v)) {
                                gc.setFill(cmap.apply(xy.v));
                            } else {
                                gc.setFill(color);
                            }

                            gc.setEffect(textEffect);
                            gc.fillText(text, x, y);
                            gc.setEffect(null);
                            if (showAnchorPoint) {
                                gc.strokeLine(x - 5, y, x + 5, y);
                                gc.strokeLine(x, y - 5, x, y + 5);
                            }
                            if (showTextBounds) {
                                var b = boundOfText(null, text, x, y);
                                gc.strokeRect(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
                            }
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

        public Builder showAnchorPoint(boolean show) {
            graphics.showAnchorPoint(show);
            return this;
        }

        public Builder showTextBounds(boolean show) {
            graphics.showTextBounds(show);
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

        public Builder addText(String text, double x, double y, double v) {
            graphics.addData(new XY(x, y, v, text));
            return this;
        }

        public Builder addText(String text, Point2D p) {
            graphics.addData(new XY(p, text));
            return this;
        }

        public Builder addText(String text, Point2D p, double v) {
            graphics.addData(new XY(p, v, text));
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
            graphics.addData(new XY(px, py, data));
            return this;
        }

        public Builder addText(String text, double x, double y, double v, double px, double py) {
            var data = new XY(x, y, v, text);
            graphics.addData(data);
            graphics.addData(new XY(px, py, data));
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
            graphics.addData(new XY(a, data));
            return this;
        }

        public Builder addText(String text, Point2D p, double v, Point2D a) {
            var data = new XY(p, v, text);
            graphics.addData(data);
            graphics.addData(new XY(a, data));
            return this;
        }

        public Builder clearTexts() {
            graphics.clearData();
            return this;
        }
    }
}
