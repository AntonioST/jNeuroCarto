package io.ast.jneurocarto.javafx.blueprint;

import java.util.*;
import java.util.function.DoubleBinaryOperator;

import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.blueprint.Blueprint;

@NullMarked
public class BlueprintPaintingHandle<T> {

    record Legend(int category, String name, Color color) {
    }

    private @Nullable T channelmap;
    private @Nullable Blueprint<T> blueprint;
    private final Map<BlueprintPainter.Feature, Boolean> features = new EnumMap<>(BlueprintPainter.Feature.class);

    final List<Legend> legends = new ArrayList<>();

    double x;
    double y;
    double w;
    double h;

    @Nullable
    DoubleBinaryOperator transform;


    BlueprintPaintingHandle() {
    }

    public T channelmap() {
        return Objects.requireNonNull(channelmap);
    }

    void setChannelmap(T channelmap) {
        this.channelmap = channelmap;
    }

    public Set<BlueprintPainter.Feature> getFeatures() {
        return Collections.unmodifiableSet(features.keySet());
    }

    public boolean hasFeature(BlueprintPainter.Feature feature) {
        return features.getOrDefault(feature, false);
    }

    void setFeature(BlueprintPainter.Feature feature) {
        features.put(feature, true);
    }

    void unsetFeature(BlueprintPainter.Feature feature) {
        features.put(feature, false);
    }

    public Blueprint<T> blueprint() {
        return Objects.requireNonNull(blueprint);
    }

    void setBlueprint(Blueprint<T> blueprint) {
        channelmap = blueprint.channelmap();
        this.blueprint = blueprint;
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

    /**
     * scale value on x-axis based on each shank
     *
     * @param transform {@code (shank, x) -> x}
     */
    public void setXonShankTransform(DoubleBinaryOperator transform) {
        this.transform = transform;
    }

    public List<String> categories() {
        return legends.stream().map(Legend::name).toList();
    }

    void resetCategories() {
        legends.clear();
    }

    public void addCategory(int category, String name, Color color) {
        legends.add(new Legend(category, name, color));
    }
}
