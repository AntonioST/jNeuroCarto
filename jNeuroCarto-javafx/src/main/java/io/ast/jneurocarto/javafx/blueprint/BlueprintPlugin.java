package io.ast.jneurocarto.javafx.blueprint;

import java.lang.reflect.InvocationTargetException;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.view.InvisibleView;

public class BlueprintPlugin extends InvisibleView {

    private final CartoConfig config;
    private final ProbeDescription<?> probe;
    private ProbeView<?> view;
    private InteractionXYPainter foreground;
    private BlueprintPainter painter;

    private final Logger log = LoggerFactory.getLogger(BlueprintPlugin.class);

    public BlueprintPlugin(CartoConfig config, ProbeDescription<?> probe) {
        this.config = config;
        this.probe = probe;
    }

    @Override
    public String name() {
        return "blueprint";
    }

    @Override
    public String description() {
        return "show blueprint";
    }

    /*=================================*
     * BlueprintPainter initialization *
     *=================================*/

    private @Nullable BlueprintPainter checkBlueprintPainter(PluginSetupService service) {
        var s = service.asProbePluginSetupService();

        var painters = s.scanInterface(BlueprintPainter.class);
        if (painters.isEmpty()) {
            log.info("BlueprintPainter not found");
            visible.setValue(false);
            return null;
        } else if (painters.size() == 1) {
            var clazz = painters.get(0);
            log.debug("use {}", clazz.getName());
            return newBlueprintPainter(clazz);
        } else {
            var clazz = painters.get(0);
            log.debug("multiple BlueprintPainter found. use {}", clazz.getName());
            return newBlueprintPainter(clazz);
        }
    }

    private @Nullable BlueprintPainter newBlueprintPainter(Class<BlueprintPainter> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            log.warn("newBlueprintPainter", e);
            return null;
        }
    }

    /*============*
     * properties *
     *============*/

    /*===========*
     * UI layout *
     *===========*/

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");

        view = service.getProbeView();
        foreground = view.getForegroundPainter();
        painter = checkBlueprintPainter(service);
        if (painter == null) visible.set(false);

        return super.setup(service);
    }

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {
        return new Label("content");
    }
}
