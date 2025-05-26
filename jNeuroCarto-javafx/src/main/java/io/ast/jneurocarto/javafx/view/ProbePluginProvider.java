package io.ast.jneurocarto.javafx.view;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;

@NullMarked
public interface ProbePluginProvider {

    default String description() {
        return getClass().getSimpleName();
    }

    /**
     * @param config
     * @param desp
     * @return list of plugins. {@code null} if {@code desp} is not supported by this plugin.
     * @throws ClassCastException if {@code desp} is not supported by this plugin.
     */
    @Nullable
    List<ProbePlugin<?>> setup(CartoConfig config, ProbeDescription<?> desp);
}
