package io.ast.jneurocarto.javafx.chart.colormap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

import org.jspecify.annotations.Nullable;

public final class DiscreteColormap extends Colormap {

    private final List<Stop> stops;

    public DiscreteColormap() {
        this(null, List.of());
    }

    public DiscreteColormap(List<Stop> stops) {
        this(null, stops);
    }

    public DiscreteColormap(@Nullable String name, List<Stop> stops) {
        super(name);
        this.stops = new ArrayList<>(stops);
        this.stops.sort(Comparator.comparingDouble(Stop::getOffset));
    }

    public int addColor(Color color) {
        var last = stops.getLast();
        var offset = (int) last.getOffset() + 1;
        stops.add(new Stop(offset, color));
        return offset;
    }

    public void addColor(double offset, Color color) {
        var stop = new Stop(offset, color);
        var size = stops.size();
        for (int i = 1; i < size; i++) {
            var s2 = stops.get(i);
            if (offset < s2.getOffset()) {
                stops.add(i, stop);
            }
        }
        stops.add(stop);
    }

    @Override
    public Color apply(double t) {
        var size = stops.size();
        for (int i = 1; i < size; i++) {
            var s2 = stops.get(i);
            if (t < s2.getOffset()) {
                var s1 = stops.get(i - 1);
                return s1.getColor();
            }
        }
        return stops.getLast().getColor();
    }
}
