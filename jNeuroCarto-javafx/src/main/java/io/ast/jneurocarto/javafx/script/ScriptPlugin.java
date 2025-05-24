package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandles;

import javafx.scene.Node;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.view.GlobalStateView;
import io.ast.jneurocarto.javafx.view.InvisibleView;

public class ScriptPlugin extends InvisibleView implements GlobalStateView<ScriptConfig> {

    private final CartoConfig config;
    private final ProbeDescription<Object> probe;

    private final Logger log = LoggerFactory.getLogger(ScriptPlugin.class);

    public ScriptPlugin(CartoConfig config, ProbeDescription<?> probe) {
        this.config = config;
        this.probe = (ProbeDescription<Object>) probe;
    }

    @Override
    public String name() {
        return "script";
    }

    @Override
    public String description() {
        return "run blueprint script";
    }

    /*=================================*
     * blueprint script initialization *
     *=================================*/

    private void initBlueprintScripts(PluginSetupService service) {
        var lookup = MethodHandles.publicLookup();

        service = service.asProbePluginSetupService();
        for (var clazz : service.scanAnnotation(BlueprintScript.class)) {
            for (var handle : BlueprintScriptHandles.lookupClass(lookup, clazz)) {
                initBlueprintScript(handle);
            }
        }
    }

    private void initBlueprintScript(BlueprintScriptHandle handle) {
        log.debug("init {} = {}.{}", handle.name, handle.declaredClass.getSimpleName(), handle.declaredMethod.getName());
    }


    /*============*
     * properties *
     *============*/

    /*=================*
     * state load/save *
     *=================*/

    @Override
    public @Nullable ScriptConfig getState() {
        return null;
    }

    @Override
    public void restoreState(@Nullable ScriptConfig state) {

    }

    /*===========*
     * UI layout *
     *===========*/

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");

        initBlueprintScripts(service);

        return super.setup(service);
    }

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {
        return null;
    }

    /*==============*
     * event handle *
     *==============*/
}
