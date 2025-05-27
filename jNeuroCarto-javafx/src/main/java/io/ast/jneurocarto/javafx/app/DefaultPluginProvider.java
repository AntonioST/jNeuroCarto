package io.ast.jneurocarto.javafx.app;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.javafx.atlas.AtlasPlugin;
import io.ast.jneurocarto.javafx.blueprint.BlueprintPlugin;
import io.ast.jneurocarto.javafx.script.ScriptPlugin;
import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.javafx.view.Provide;

@NullMarked
@Provide(value = AtlasPlugin.class, name = {"atlas", "neurocarto.views.atlas:AtlasBrainView"})
@Provide(value = BlueprintPlugin.class, name = {"blueprint", "neurocarto.views.blueprint:BlueprintView"})
@Provide(value = ScriptPlugin.class, name = {"script", "neurocarto.views.blueprint_script:BlueprintScriptView"})
public class DefaultPluginProvider implements PluginProvider {
    @Override
    public String description() {
        return "provide jNeuroCarto application default plugins";
    }

    @Override
    public boolean filterPlugin(PluginSetupService service, Class<? extends Plugin> plugin) {
        if (plugin == AtlasPlugin.class) {
            var config = service.getCartoConfig();
            if (config.atlasName == null || config.atlasName.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
