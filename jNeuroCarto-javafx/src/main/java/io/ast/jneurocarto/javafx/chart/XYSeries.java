package io.ast.jneurocarto.javafx.chart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.blueprint.MinMax;

/**
 * a {@link XYGraphics} that take {@link XY} as its internal data points.
 */
@NullMarked
public abstract class XYSeries implements XYGraphics {

    protected double z = 0;
    protected double alpha = 1;
    protected boolean visible = true;
    protected List<XY> data = new ArrayList<>();

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public double z() {
        return z;
    }

    public void z(double z) {
        this.z = z;
    }

    public double alpha() {
        return alpha;
    }

    public void alpha(double alpha) {
        this.alpha = alpha;
    }

    public static Normalize renormalize(XYColormapSeries[] graphics) {
        if (graphics.length == 0) throw new RuntimeException();
        if (graphics.length == 1) return graphics[0].renormalize();

        var norm = Arrays.stream(graphics)
          .map(XYColormapSeries::renormalize)
          .gather(Normalize.union())
          .findFirst().get();

        for (var g : graphics) {
            g.normalize(norm);
        }
        return norm;
    }

    public static Normalize renormalize(XYColormapSeries[] graphics, Normalize init) {
        if (graphics.length == 0) return init;

        var stream = Arrays.stream(graphics).map(XYColormapSeries::renormalize);

        var norm = Stream.concat(Stream.of(init), stream)
          .gather(Normalize.union())
          .findFirst().get();

        for (var g : graphics) {
            g.normalize(norm);
        }

        return norm;
    }


    @Override
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Stream<XY> data() {
        return data.stream();
    }

    public void clearData() {
        data.clear();
    }

    public void addGap() {
        data.add(XY.GAP);
    }

    public XY addData(double x, double y) {
        return addData(x, y, 0.0);
    }

    public XY addData(double x, double y, double v) {
        var ret = new XY(x, y, v);
        data.add(ret);
        return ret;
    }

    public XY addData(Point2D p) {
        return addData(p, 0.0);
    }

    public XY addData(Point2D p, double v) {
        var ret = new XY(p, v);
        data.add(ret);
        return ret;
    }

    public void addData(XY data) {
        this.data.add(data);
    }

    public void addData(List<XY> data) {
        this.data.addAll(data);
    }

    public void addData(Stream<XY> data) {
        data.forEach(this.data::add);
    }

    public void copyData(XYSeries other) {
        other.data.addAll(data);
    }

    public void copyData(XYSeries other, Predicate<XY> tester) {
        for (XY xy : data) {
            if (tester.test(xy)) {
                other.addData(xy);
            }
        }
    }

    public void transferData(XYSeries other) {
        other.data.addAll(data);
        data.clear();
    }

    public void transferData(XYSeries other, Predicate<XY> tester) {
        var iter = data.iterator();
        while (iter.hasNext()) {
            var xy = iter.next();
            if (tester.test(xy)) {
                other.addData(xy);
                iter.remove();
            }
        }
    }

    public XY removeData(int index) {
        return data.remove(index);
    }

    public List<XY> removeData(int index, int length) {
        var ret = new ArrayList<XY>(length);
        var iter = data.listIterator(index);
        for (int i = 0; i < length; i++) {
            ret.add(iter.next());
            iter.remove();
        }
        return ret;
    }

    public List<XY> removeData(Predicate<XY> tester) {
        var ret = new ArrayList<XY>(size());
        var iter = data.iterator();
        while (iter.hasNext()) {
            var xy = iter.next();
            if (tester.test(xy)) {
                ret.add(xy);
                iter.remove();
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Only use 3 columns, which put transformed {@link XY#x()}, transformed {@link XY#y()},
     * and {@link XY#v()}, respectively.
     * <p/>
     *
     * @param aff {@link GraphicsContext}'s affine transformation.
     * @param p   {@code double[4][row]} array that store the transformed data.
     * @return number of row used.
     */
    @Override
    public int transform(Affine aff, double[][] p) {
        var data = this.data;
        var length = data.size();

        for (int i = 0; i < length; i++) {
            var xy = data.get(i);
            var q = aff.transform(xy.x, xy.y);
            p[0][i] = q.getX();
            p[1][i] = q.getY();
            p[2][i] = xy.v;
        }

        return length;
    }

    /*=========*
     * builder *
     *=========*/

    public <B extends Builder<XYSeries, B>> Builder<XYSeries, B> builder() {
        return new Builder<>(this);
    }

    public static class Builder<S extends XYSeries, B extends Builder<S, B>> {
        protected final S graphics;

        protected Builder(S graphics) {
            this.graphics = graphics;
        }

        public S graphics() {
            return graphics;
        }

        public B z(double z) {
            graphics.z(z);
            return (B) this;
        }

        public B alpha(double alpha) {
            graphics.alpha(alpha);
            return (B) this;
        }

        public B setVisible(boolean visible) {
            graphics.setVisible(visible);
            return (B) this;
        }
    }

    /*==========*
     * subclass *
     *==========*/

    public static abstract class XYColormapSeries extends XYSeries {
        protected @Nullable Colormap colormap = null;

        public @Nullable Colormap colormap() {
            return colormap;
        }

        /**
         * set colormap.
         *
         * @param colormap
         */
        public void colormap(Colormap colormap) {
            this.colormap = colormap;
        }

        public @Nullable Normalize normalize() {
            var cm = colormap;
            return cm == null ? null : cm.normalize();
        }

        public void normalize(Normalize normalize) {
            var cm = Objects.requireNonNull(colormap, "miss colormap");
            colormap = cm.withNormalize(normalize);
        }

        public Normalize renormalize() {
            if (data.size() < 2) {
                return Normalize.N01;
            } else {
                var result = data.stream()
                  .mapToDouble(XY::v)
                  .boxed()
                  .gather(MinMax.doubleMinmax())
                  .findFirst()
                  .get();

                return new Normalize(result);
            }
        }
    }

    public static class XYColormapSeriesBuilder<S extends XYColormapSeries, B extends XYColormapSeriesBuilder<S, B>> extends Builder<S, B> {

        public XYColormapSeriesBuilder(S graphics) {
            super(graphics);
        }

        public B colormap(String colormap) {
            graphics.colormap(Colormap.of(colormap));
            return (B) this;
        }

        public B colormap(Colormap colormap) {
            graphics.colormap(colormap);
            return (B) this;
        }

        public B normalize(double upper) {
            return normalize(0, upper);
        }

        public B normalize(double lower, double upper) {
            return normalize(new Normalize(lower, upper));
        }

        public B normalize(Normalize normalize) {
            var colormap = Objects.requireNonNull(graphics.colormap, "miss a colormap");
            graphics.colormap(colormap.withNormalize(normalize));
            return (B) this;
        }
    }
}
