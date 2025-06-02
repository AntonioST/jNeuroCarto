package io.ast.jneurocarto.javafx.chart.colormap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleFunction;

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed abstract class Colormap implements DoubleFunction<Color>
    permits LinearColormap, DiscreteColormap {

    private final @Nullable String name;

    protected Colormap(@Nullable String name) {
        this.name = name;
    }

    public static List<String> availableBuiltinColormapName() {
        return new ArrayList<>(ColormapPlt.COLORMAPS.keySet());
    }

    public static Colormap of(Color color) {
        return new LinearColormap(List.of(
            new Stop(0, color),
            new Stop(1, color)
        ));
    }

    public static LinearColormap of(String name) {
        var ret = ColormapPlt.COLORMAPS.get(name);
        if (ret != null) return new LinearColormap(name, ret.stops, Normalize.N01);

        if (name.endsWith("_r")) {
            ret = of(name.substring(0, name.length() - 2));

            var stops = Arrays.asList(ret.stops).reversed().stream()
                .map(stop -> new Stop(1 - stop.getOffset(), stop.getColor()))
                .toList();

            return new LinearColormap(name, stops);
        }

        throw new IllegalArgumentException("unknown colormap " + name);
    }

    public @Nullable String name() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (name == null ? "[?]" : "[" + name + "]");
    }
}
