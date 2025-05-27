package io.ast.jneurocarto.javafx.chart;

import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class Colormap {

    private final List<Stop> stops;

    public Colormap(List<Stop> stops) {
        if (stops.size() < 2) throw new RuntimeException();
        var gradient = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops);
        this.stops = gradient.getStops();
    }

    public static Colormap of(String name) {
        var ret = ColormapPlt.COLORMAPS.get(name);
        if (ret != null) return ret;
        if (name.endsWith("_r")) {
            ret = of(name.substring(0, name.length() - 2));
            return new Colormap(ret.stops.reversed().stream()
              .map(stop -> new Stop(1 - stop.getOffset(), stop.getColor()))
              .toList());
        }
        throw new IllegalArgumentException("unknown colormap " + name);
    }

    public Color get(double t) {
        return get(Normalize.N01, t);
    }

    public LinearGradient get(double x1, double y1, double x2, double y2, double t1, double t2) {
        return get(x1, y1, x2, y2, Normalize.N01, t1, t2);
    }

    public Color get(Normalize normalize, double t) {
        var t1 = normalize.applyAsDouble(t);
        var size = stops.size();
        for (int i = 1; i < size; i++) {
            var s2 = stops.get(i);
            if (t1 < s2.getOffset()) {
                var s1 = stops.get(i - 1);
                var t2 = (t1 - s1.getOffset()) / (s2.getOffset() - s1.getOffset());
                return s1.interpolate(s2, t2).getColor();
            }
        }
        return stops.get(size - 1).getColor();
    }

    public LinearGradient get(double x1, double y1, double x2, double y2, Normalize normalize, double t1, double t2) {
        var c1 = get(normalize, t1);
        var c2 = get(normalize, t2);
        return new LinearGradient(x1, y1, x2, y2, false, CycleMethod.NO_CYCLE, new Stop(0, c1), new Stop(1, c2));
    }
}
