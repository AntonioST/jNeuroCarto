package io.ast.jneurocarto.javafx.atlas;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.Coordinate;

@NullMarked
public record AtlasReference(String atlasNams, String name, Coordinate coordinate, boolean flipAP) {
}
