package io.ast.jneurocarto.javafx.view;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public interface ProbePluginProvider {

    <T> List<ProbePlugin<T>> setup(CartoConfig config, ProbeDescription<T> desp);
}
