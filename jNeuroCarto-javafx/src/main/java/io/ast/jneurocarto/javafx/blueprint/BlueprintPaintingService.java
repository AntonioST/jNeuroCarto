package io.ast.jneurocarto.javafx.blueprint;

import java.util.Collections;
import java.util.List;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.core.blueprint.Blueprint;

public class BlueprintPaintingService<T> {

    public final Blueprint<T> blueprint;
    private final List<BlueprintPainter.Feature> features;

    BlueprintPaintingService(Blueprint<T> toolkit, List<BlueprintPainter.Feature> features) {
        this.blueprint = toolkit;
        this.features = Collections.unmodifiableList(features);
    }

    public T getChannelmap() {
        return blueprint.channelmap();
    }

    public List<BlueprintPainter.Feature> getFeatures() {
        return features;
    }

    public boolean hasFeature(BlueprintPainter.Feature feature) {
        return features.contains(feature);
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
