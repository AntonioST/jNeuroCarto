package io.ast.jneurocarto.javafx.app;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public record RequestChannelmapType<T>(Class<ProbeDescription<T>> probe, @Nullable String code) {
}
