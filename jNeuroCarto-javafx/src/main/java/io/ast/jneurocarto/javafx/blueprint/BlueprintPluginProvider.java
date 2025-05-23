package io.ast.jneurocarto.javafx.blueprint;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.view.PluginProvider;

@NullMarked
public class BlueprintPluginProvider implements PluginProvider {
    @Override
    public List<String> name() {
        return List.of(
          "blueprint",
          "neurocarto.views.blueprint:BlueprintView" // python name
        );
    }

    @Override
    public BlueprintPlugin setup(CartoConfig config, ProbeDescription<?> desp) {
        return new BlueprintPlugin(config, desp);
    }
}
