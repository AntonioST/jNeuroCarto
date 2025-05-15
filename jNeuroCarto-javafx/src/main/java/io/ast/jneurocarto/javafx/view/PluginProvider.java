package io.ast.jneurocarto.javafx.view;

import java.util.List;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ProbeDescription;

public interface PluginProvider {

    List<Plugin> get(CartoConfig config, ProbeDescription<?> desp);
}
