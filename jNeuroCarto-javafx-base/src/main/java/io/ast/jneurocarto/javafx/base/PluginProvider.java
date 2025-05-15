package io.ast.jneurocarto.javafx.base;

import java.util.List;

import io.ast.jneurocarto.core.ProbeDescription;

public interface PluginProvider {

    List<Plugin> get(ProbeDescription<?> desp);
}
