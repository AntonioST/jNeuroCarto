package io.ast.jneurocarto.javafx.script;

import java.util.List;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.PluginProvider;

public class ScriptPluginProvider implements PluginProvider {
    @Override
    public List<String> name() {
        return List.of(
          "script",
          "neurocarto.views.blueprint_script:BlueprintScriptView"  // python name
        );
    }

    @Override
    public Plugin setup(CartoConfig config, ProbeDescription<?> desp) {
        return new ScriptPlugin(config, desp);
    }
}
