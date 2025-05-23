package io.ast.jneurocarto.javafx.blueprint;

import java.util.Collections;
import java.util.List;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.core.blueprint.Blueprint;

public class BlueprintPaintingService<T> {

    public final Blueprint<T> blueprint;
    private final List<String> options;

    BlueprintPaintingService(Blueprint<T> toolkit, List<String> options) {
        this.blueprint = toolkit;
        this.options = Collections.unmodifiableList(options);
    }

    public T getChannelmap() {
        return blueprint.channelmap();
    }

    public List<String> getOptions() {
        return options;
    }

    public boolean hasOptions(String option) {
        return options.contains(option);
    }

    /**
     * @param dx
     * @param dy
     * @see io.ast.jneurocarto.core.blueprint.ClusteringEdges#setCorner(double, double)
     */
    public void setCorner(double dx, double dy) {
    }

    /**
     * @param dx
     * @param dy
     * @see io.ast.jneurocarto.core.blueprint.ClusteringEdges#offset(double, double)
     */
    public void setOffset(double dx, double dy) {

    }

    public void addCategory(int category, String name, Color color) {
    }


}
