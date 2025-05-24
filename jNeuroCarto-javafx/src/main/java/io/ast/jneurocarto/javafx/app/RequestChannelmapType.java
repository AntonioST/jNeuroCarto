package io.ast.jneurocarto.javafx.app;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public record RequestChannelmapType(Class<? extends ProbeDescription> probe, @Nullable String code, boolean create) {
    public RequestChannelmapType(Class<? extends ProbeDescription> probe, @Nullable String code) {
        this(probe, code, true);
    }
}
