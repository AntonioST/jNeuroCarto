package io.ast.jneurocarto.javafx.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.core.blueprint.Blueprint;

public class BlueprintPaintingService<T> {

    record Legend(int category, String name, Color color) {
    }

    public final Blueprint<T> blueprint;
    private final Set<BlueprintPainter.Feature> features;

    final List<Legend> legends = new ArrayList<>();

    double x;
    double y;
    double w;
    double h;


    BlueprintPaintingService(Blueprint<T> toolkit, Set<BlueprintPainter.Feature> features) {
        this.blueprint = toolkit;
        this.features = features;
    }

    public T getChannelmap() {
        return blueprint.channelmap();
    }

    public Set<BlueprintPainter.Feature> getFeatures() {
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
        w = dx;
        h = dy;
    }

    /**
     * @param dx
     * @param dy
     * @see io.ast.jneurocarto.core.blueprint.ClusteringEdges#offset(double, double)
     */
    public void setOffset(double dx, double dy) {
        x = dx;
        y = dy;
    }

    public List<String> categories() {
        return legends.stream().map(Legend::name).toList();
    }

    public void addCategory(int category, String name, Color color) {
        legends.add(new Legend(category, name, color));
    }


}
