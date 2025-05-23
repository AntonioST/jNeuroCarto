package io.ast.jneurocarto.javafx.atlas;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.view.PluginProvider;

@NullMarked
public class AtlasPluginProvider implements PluginProvider {
    @Override
    public List<String> name() {
        return List.of(
          "atlas",
          "neurocarto.views.atlas:AtlasBrainView" // python name
        );
    }

    @Override
    public AtlasPlugin setup(CartoConfig config, ProbeDescription<?> desp) {
        return new AtlasPlugin(config);
    }
}
