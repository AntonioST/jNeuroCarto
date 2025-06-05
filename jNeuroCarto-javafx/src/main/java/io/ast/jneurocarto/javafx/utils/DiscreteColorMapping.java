package io.ast.jneurocarto.javafx.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.javafx.chart.colormap.DiscreteColormap;

public class DiscreteColorMapping implements ToDoubleFunction<String>, ToIntFunction<String> {

    private final Map<String, Integer> colorMapping = new HashMap<>();
    public final DiscreteColormap colormap = new DiscreteColormap();

    public void put(String name, Color color) {
        colormap.addColor(color);
        colorMapping.put(name, colorMapping.size());
    }

    @Override
    public double applyAsDouble(String color) {
        return applyAsInt(color);
    }

    @Override
    public int applyAsInt(String color) {
        return colorMapping.computeIfAbsent(color, name -> colormap.addColor(Color.valueOf(name)));
    }
}
