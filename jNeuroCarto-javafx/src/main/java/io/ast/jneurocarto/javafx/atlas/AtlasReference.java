package io.ast.jneurocarto.javafx.atlas;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.ProbeTransform;

@NullMarked
public record AtlasReference(String atlasNams, String name, Coordinate coordinate, boolean flipAP) {

    /**
     * {@return a transform from global anatomical space to referenced anatomical space.}
     */
    public ProbeTransform<Coordinate, Coordinate> getTransform() {
        return ProbeTransform.create(name, coordinate, flipAP);
    }
}
