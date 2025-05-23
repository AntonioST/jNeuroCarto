package io.ast.jneurocarto.javafx.view;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;

@NullMarked
public interface ProbePluginProvider {

    List<ProbePlugin<?>> setup(CartoConfig config, ProbeDescription<?> desp);
}
