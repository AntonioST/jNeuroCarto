package io.ast.jneurocarto.javafx.script;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.view.GlobalStateView;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.ClassInfo;

public class ScriptPlugin extends InvisibleView implements GlobalStateView<ScriptConfig> {

    private final CartoConfig config;
    private final ProbeDescription<Object> probe;
    private final List<BlueprintScriptCallable> functinos = new ArrayList<>();

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
        for (var clazz : service.scanAnnotation(BlueprintScript.class, this::filterBlueprintScript)) {
            for (var handle : BlueprintScriptHandles.lookupClass(lookup, clazz)) {
                initBlueprintScript(handle);
            }
        }
    }

    private boolean filterBlueprintScript(ClassInfo info) {
        var ann = info.getAnnotationInfo(BlueprintScript.class);
        String name;
        if (ann == null) {
            name = info.getSimpleName();
        } else {
            name = (String) ann.getParameterValues().getValue("value");
        }
        log.debug("filter \"{}\" = {}", name, info.getName());

        var checkAnn = info.getAnnotationInfo(CheckProbe.class);
        if (checkAnn == null) return true;

        var check = checkAnn.getParameterValues();
        var family = (String) check.getValue("value");

        var probeValue = check.getValue("probe");
        Class<? extends ProbeDescription> probe;
        if (probeValue instanceof AnnotationClassRef ref) {
            var refClass = ref.loadClass(true);
            if (refClass == null) {
                log.debug("unknown probe() class {}", ref.getName());
                return false;
            } else if (ProbeDescription.class.isAssignableFrom(refClass)) {
                probe = (Class<? extends ProbeDescription>) refClass;
            } else {
                log.debug("illegal probe() class {}", ref.getName());
                return false;
            }
        } else {
            log.debug("unknown probe() value {}", probeValue);
            return false;
        }

        if (probe == ProbeDescription.class) {
            if (family.isEmpty()) return true;

            var ret = ProbeDescription.getProbeDescription(family);
            if (ret == null) {
                log.debug("unknown value(), probe {} not found.", family);
                return false;
            }
            probe = ret.getClass();
        }

        var ret = this.probe.getClass().isAssignableFrom(probe);
        if (!ret) {
            log.debug("reject probe {}", probe.getName());
            return false;
        }
        return ret;
    }

    private void initBlueprintScript(BlueprintScriptCallable callable) {
        if (callable instanceof BlueprintScriptHandle handle) {
            log.debug("init {} = {}.{}", handle.name, handle.declaredClass.getSimpleName(), handle.declaredMethod.getName());
        } else {
            log.debug("init {} = {}.{}", callable.name());
        }
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

    /*=================*
     * script invoking *
     *=================*/
}
