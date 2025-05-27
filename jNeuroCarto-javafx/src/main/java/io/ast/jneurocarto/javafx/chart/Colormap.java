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
        var ret = ColormapK3d.COLORMAPS.get(name);
        if (ret != null) return ret;

        return switch (name) {
            case "jet" -> JET;
            case "plasma" -> PLASMA;
            case "inferno" -> INFERNO;
            case "viridis" -> VIRIDIS;
            case "cividis" -> CIVIDIS;
            default -> throw new IllegalArgumentException();
        };
    }

    public Color get(double t) {
        return get(Normalize.N01, t);
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

    public static final Colormap JET = new Colormap(List.of(
      new Stop(0.0, Color.rgb(0, 0, 131)),
      new Stop(0.125, Color.rgb(0, 60, 170)),
      new Stop(0.375, Color.rgb(5, 255, 255)),
      new Stop(0.625, Color.rgb(255, 255, 0)),
      new Stop(0.875, Color.rgb(250, 0, 0)),
      new Stop(1.0, Color.rgb(128, 0, 0))
    ));

    public static final Colormap PLASMA = new Colormap(List.of(
      new Stop(0.0, Color.rgb(13, 8, 135)),
      new Stop(0.25, Color.rgb(126, 3, 167)),
      new Stop(0.5, Color.rgb(203, 71, 119)),
      new Stop(0.75, Color.rgb(248, 149, 64)),
      new Stop(1.0, Color.rgb(240, 249, 33))
    ));

    public static final Colormap INFERNO = new Colormap(List.of(
      new Stop(0.0, Color.rgb(0, 0, 4)),
      new Stop(0.25, Color.rgb(87, 15, 109)),
      new Stop(0.5, Color.rgb(187, 55, 84)),
      new Stop(0.75, Color.rgb(249, 142, 8)),
      new Stop(1.0, Color.rgb(252, 255, 164))
    ));

    public static final Colormap VIRIDIS = new Colormap(List.of(
      new Stop(0.0, Color.rgb(68, 1, 84)),
      new Stop(0.25, Color.rgb(59, 82, 139)),
      new Stop(0.5, Color.rgb(33, 145, 140)),
      new Stop(0.75, Color.rgb(94, 201, 98)),
      new Stop(1.0, Color.rgb(253, 231, 37))
    ));

    public static final Colormap CIVIDIS = new Colormap(List.of(
      new Stop(0.0, Color.rgb(0, 32, 76)),
      new Stop(0.25, Color.rgb(55, 80, 120)),
      new Stop(0.5, Color.rgb(120, 130, 130)),
      new Stop(0.75, Color.rgb(175, 180, 100)),
      new Stop(1.0, Color.rgb(255, 233, 69))
    ));
}
