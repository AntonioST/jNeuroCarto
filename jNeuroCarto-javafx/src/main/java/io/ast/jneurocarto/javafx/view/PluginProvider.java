package io.ast.jneurocarto.javafx.view;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;

@NullMarked
public interface PluginProvider {

    default List<String> name() {
        return List.of();
    }

    default String description() {
        return getClass().getSimpleName();
    }

    Plugin setup(CartoConfig config, ProbeDescription<?> desp);
}
