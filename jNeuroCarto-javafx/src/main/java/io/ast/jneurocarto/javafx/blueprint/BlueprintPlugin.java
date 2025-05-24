package io.ast.jneurocarto.javafx.blueprint;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.blueprint.ClusteringEdges;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbePlugin;

public class BlueprintPlugin extends InvisibleView implements ProbePlugin<Object> {

    private final CartoConfig config;
    private final ProbeDescription<?> probe;
    private ProbeView<?> view;
    private InteractionXYPainter foreground;
    private BlueprintPainter<?> painter;

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

    public final DoubleProperty alphaProperty = new SimpleDoubleProperty(0.5);

    public final double getAlpha() {
        return alphaProperty.get();
    }

    public final void setAlpha(double value) {
        alphaProperty.set(value);
    }

    public final BooleanProperty conflictProperty = new SimpleBooleanProperty();

    public final boolean isConflict() {
        return conflictProperty.get();
    }

    public final void setConflict(boolean value) {
        conflictProperty.set(value);
    }

    public Set<BlueprintPainter.Feature> getFeatures() {
        var ret = new EnumMap<BlueprintPainter.Feature, Boolean>(BlueprintPainter.Feature.class);
        if (conflictProperty.get()) ret.put(BlueprintPainter.Feature.conflict, true);
        return ret.keySet();
    }

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
    protected HBox setupHeading(PluginSetupService service) {
        var layout = super.setupHeading(service);

        var painter = this.painter;
        if (painter != null) {
            var features = painter.supportedFeatures();
            if (features.contains(BlueprintPainter.Feature.conflict)) {
                var conflictSwitch = new CheckBox("Conflict");
                conflictSwitch.selectedProperty().bindBidirectional(conflictProperty);
                layout.getChildren().add(conflictSwitch);
            }
        }

        return layout;
    }

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {
        return new Label("content");
    }

    /*===================*
     * blueprint drawing *
     *===================*/

    private Blueprint<Object> blueprint;
    private Object channelmap;
    private List<ElectrodeDescription> electrodes;

    @Override
    public void onProbeUpdate(Object chmap, List<ElectrodeDescription> blueprint) {
        channelmap = chmap;
        electrodes = blueprint;
        if (this.blueprint == null || this.blueprint.sameChannelmapCode(chmap)) {
            this.blueprint = new Blueprint<>((ProbeDescription<Object>) probe, channelmap, blueprint);
        } else {
            this.blueprint = new Blueprint<>(this.blueprint, chmap);
            this.blueprint.from(blueprint);
        }
        updateBlueprint(this.blueprint);
    }

    private void updateBlueprint(Blueprint<Object> blueprint) {
        if (!visible.get() || painter == null) return;

        var service = new BlueprintPaintingService<>(blueprint, getFeatures());
        ((BlueprintPainter<Object>) painter).plotBlueprint(service);

        foreground.retainSeries(service.categories());

        var tool = new BlueprintToolkit<>(blueprint);
        for (var legend : service.legends) {
            var series = foreground.getOrNewSeries(legend.name());
            series.alpha(alphaProperty.get());
            series.fill(legend.color());

            series.clearData();
            for (var clustering : tool.getClusteringEdges(legend.category())) {
                var transform = service.transform;
                if (transform != null) {
                    var shank = clustering.shank();
                    clustering = clustering.map(corner -> {
                        var x = transform.applyAsDouble(shank, corner.x());
                        return new ClusteringEdges.Corner(x, corner.y(), corner.corner());
                    });
                }

                clustering = clustering.offset(service.x, service.y).setCorner(service.w, service.h);

                clustering.edges().forEach(c -> series.addData(c.x(), c.y()));

                var c = clustering.edges().get(0);
                series.addData(c.x(), c.y());
                series.addGap();
            }
        }

        foreground.repaint();
    }
}
