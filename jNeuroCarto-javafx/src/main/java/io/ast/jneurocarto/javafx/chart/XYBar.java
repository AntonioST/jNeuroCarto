package io.ast.jneurocarto.javafx.chart;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToDoubleFunction;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class XYBar extends XYSeries {

    public enum Orientation {
        horzontial, vertical
    }

    protected double width = 1;
    protected Orientation orientation = Orientation.vertical;
    protected @Nullable Color fill = null;

    public double width() {
        return width;
    }

    public void width(double width) {
        this.width = width;
    }

    public Orientation orientation() {
        return orientation;
    }

    public void orientation(Orientation orientation) {
        this.orientation = orientation;
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
     * number of data points.
     *
     * @return always 0. It has no points need to be transformed.
     */
    @Override
    public int points() {
        return 0;
    }

    @Override
    public int transform(Affine aff, double[][] p) {
        return 0;
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        if (fill == null) return;

        gc.setFill(fill);

        var oldAlpha = gc.getGlobalAlpha();
        try {
            gc.setGlobalAlpha(alpha);

            for (var xy : data) {
                double x = xy.x;
                double y = xy.y;

                if (Double.isNaN(x) || Double.isNaN(y)) {
                    continue;
                }

                double w;
                double h;

                if (orientation == Orientation.vertical) {
                    x -= width / 2;
                    w = width;
                    h = xy.v;
                    if (h < 0) {
                        y += h;
                        h = -h;
                    }
                } else {
                    y -= width / 2;
                    w = xy.v;
                    h = width;
                    if (w < 0) {
                        x += w;
                        w = -w;
                    }
                }

                gc.fillRect(x, y, w, h);
            }

        } finally {
            gc.setGlobalAlpha(oldAlpha);
        }
    }


    /*=========*
     * builder *
     *=========*/

    public Builder builder(double step) {
        return new Builder(this, step);
    }

    public static class Builder extends XYSeries.Builder<XYBar, Builder> {
        private double step;
        private double ratio = Double.NaN;
        private double baseline;

        public Builder(XYBar graphics, double step) {
            if (step == 0) throw new IllegalArgumentException();
            super(graphics);
            this.step = step;
        }

        public Builder width(double width) {
            graphics.width(width);
            ratio = Double.NaN;
            return this;
        }

        public Builder widthRatio(double width) {
            graphics.width(step * width);
            ratio = width;
            return this;
        }

        public Builder fitInRange(double lower, double upper) {
            if (!(lower < upper)) throw new IllegalArgumentException();
            var n = graphics.data.size();
            step = (upper - lower) / n;
            if (!Double.isNaN(ratio)) {
                graphics.width(step * ratio);
            }
            restep(lower + step / 2, step);
            return this;
        }

        public Builder fill(String fill) {
            return fill(Color.valueOf(fill));
        }

        public Builder fill(@Nullable Color fill) {
            graphics.fill(fill);
            return this;
        }

        private double p0() {
            ToDoubleFunction<XY> mapper = graphics.orientation == Orientation.vertical ? XY::x : XY::y;
            var stream = graphics.data().mapToDouble(mapper);
            var ret = step > 0 ? stream.min() : stream.max();
            return ret.orElse(0);
        }

        private double p1() {
            ToDoubleFunction<XY> mapper = graphics.orientation == Orientation.vertical ? XY::x : XY::y;
            var stream = graphics.data().mapToDouble(mapper);
            var ret = step > 0 ? stream.max() : stream.min();
            return ret.orElse(0);
        }

        public Builder baseline(double b) {
            baseline = b;

            if (graphics.orientation == Orientation.vertical) {
                for (var xy : graphics.data) {
                    xy.y(b);
                }
            } else {
                for (var xy : graphics.data) {
                    xy.x(b);
                }
            }

            return this;
        }

        public Builder step(double step) {
            if (step == 0) throw new IllegalArgumentException();
            this.step = step;

            var vertical = graphics.orientation == Orientation.vertical;
            ToDoubleFunction<XY> mapper = vertical ? XY::x : XY::y;
            var s0 = graphics.data().mapToDouble(mapper).min().orElse(0);

            restep(s0, step);

            return this;
        }

        private void restep(double zero, double step) {
            var vertical = graphics.orientation == Orientation.vertical;
            ToDoubleFunction<XY> mapper = vertical ? XY::x : XY::y;

            var i = new AtomicInteger(0);
            graphics.data().sorted(Comparator.comparingDouble(mapper)).forEach(d -> {
                if (vertical) {
                    d.x(zero + step * i.getAndIncrement());
                } else {
                    d.y(zero + step * i.getAndIncrement());
                }
            });
        }

        public Builder addData(double v) {
            return addData(p1() + step, baseline, v);
        }

        public Builder addData(double p, double v) {
            return addData(p, baseline, v);
        }

        public Builder addData(double p, double b, double v) {
            if (graphics.orientation == Orientation.vertical) {
                graphics.addData(p, b, v);
            } else {
                graphics.addData(b, p, v);
            }
            return this;
        }

        public Builder addData(double[] v) {
            return addData(p1() + step, baseline, v);
        }

        public Builder addData(double p, double[] v) {
            return addData(p, baseline, v);
        }

        public Builder addData(double p, double b, double[] v) {
            if (graphics.orientation == Orientation.vertical) {
                for (int i = 0, length = v.length; i < length; i++) {
                    graphics.addData(p + i * step, b, v[i]);
                }
            } else {
                for (int i = 0, length = v.length; i < length; i++) {
                    graphics.addData(b, p + i * step, v[i]);
                }
            }
            return this;
        }

    }

}
