package io.ast.jneurocarto.javafx.chart;

import java.util.Arrays;
import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class LinearColormap extends Colormap {

    final Stop[] stops;
    final Normalize normalize;

    public LinearColormap(List<Stop> stops) {
        this(null, stops);
    }

    public LinearColormap(@Nullable String name, List<Stop> stops) {
        var gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        this(name, gradient.getStops().toArray(Stop[]::new), Normalize.N01);
    }

    LinearColormap(@Nullable String name, Stop[] stops, Normalize normalize) {
        if (stops.length < 2) throw new RuntimeException();
        super(name);
        this.stops = stops;
        this.normalize = normalize;
    }

    public Normalize normalize() {
        return normalize;
    }

    public LinearColormap withNormalize(double upper) {
        return withNormalize(0, upper);
    }

    public LinearColormap withNormalize(double lower, double upper) {
        return withNormalize(new Normalize(lower, upper));
    }

    public LinearColormap withNormalize(Normalize normalize) {
        return new LinearColormap(name(), stops, normalize);
    }

    @Override
    public Color apply(double t) {
        var t1 = normalize.applyAsDouble(t);
        var size = stops.length;
        for (int i = 1; i < size; i++) {
            var s2 = stops[i];
            if (t1 < s2.getOffset()) {
                var s1 = stops[i - 1];
                var t2 = (t1 - s1.getOffset()) / (s2.getOffset() - s1.getOffset());
                return s1.interpolate(s2, t2).getColor();
            }
        }
        return stops[size - 1].getColor();
    }

    public LinearGradient gradient(double x1, double y1, double x2, double y2) {
        return new LinearGradient(x1, y1, x2, y2, false, CycleMethod.NO_CYCLE, Arrays.asList(stops));
    }

    public LinearGradient gradient(double x1, double y1, double x2, double y2, double t1, double t2) {
        var c1 = apply(t1);
        var c2 = apply(t2);
        return new LinearGradient(x1, y1, x2, y2, false, CycleMethod.NO_CYCLE, new Stop(0, c1), new Stop(1, c2));
    }

    @Override
    public String toString() {
        var name = name();
        return "LinearColormap" + (name == null ? "" : "[" + name + "]") + "{%.1f,%1f}".formatted(normalize.lower(), normalize.upper());
    }
}
