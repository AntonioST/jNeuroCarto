package io.ast.jneurocarto.javafx.blueprint;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

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
import io.ast.jneurocarto.javafx.chart.XYPath;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbePlugin;

public class BlueprintPlugin extends InvisibleView implements ProbePlugin<Object> {

    private final CartoConfig config;
    private final ProbeDescription<Object> probe;
    private ProbeView<?> view;
    private InteractionXYPainter foreground;
    private BlueprintPainter<Object> painter;
    private Map<String, XYPath> categories = new HashMap<>();

    private final BlueprintPaintingHandle<Object> handle = new BlueprintPaintingHandle<>();

    private final Logger log = LoggerFactory.getLogger(BlueprintPlugin.class);

    public BlueprintPlugin(CartoConfig config, ProbeDescription<?> probe) {
        this.config = config;
        this.probe = (ProbeDescription<Object>) probe;
    }

    @Override
    public String name() {
        return "Blueprint";
    }

    /*=================================*
     * BlueprintPainter initialization *
     *=================================*/

    private @Nullable BlueprintPainter<?> checkBlueprintPainter(PluginSetupService service) {
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

    private @Nullable BlueprintPainter<?> newBlueprintPainter(Class<BlueprintPainter> clazz) {
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

    {
        conflictProperty.addListener(listenOn(BlueprintPainter.Feature.conflict));
    }

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

    private ChangeListener<Boolean> listenOn(BlueprintPainter.Feature feature) {
        return (_, _, value) -> {
            if (value) {
                handle.setFeature(feature);
            } else {
                handle.unsetFeature(feature);
            }
            updateCategories();
        };
    }

    /*===========*
     * UI layout *
     *===========*/

    private HBox legendLayout;

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        view = service.getProbeView();
        foreground = view.getForegroundPainter();
        painter = (BlueprintPainter<Object>) checkBlueprintPainter(service);
        if (painter == null) visible.set(false);

        return super.setup(service);
    }

    @Override
    protected HBox setupHeading(PluginSetupService service) {
        var layout = super.setupHeading(service);

        var painter = this.painter;
        if (painter != null) {
            var features = painter.supportedFeatures();
            if (!features.isEmpty()) {
                layout.setSpacing(5);
                layout.getChildren().add(new Label("Features:"));

                if (features.contains(BlueprintPainter.Feature.conflict)) {
                    var conflictSwitch = new CheckBox("Conflict");
                    conflictSwitch.selectedProperty().bindBidirectional(conflictProperty);
                    layout.getChildren().add(conflictSwitch);
                }
            }
        }

        return layout;
    }

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {
        legendLayout = new HBox();
        legendLayout.setSpacing(5);
        updateCategories();
        return legendLayout;
    }

    private void updateLegends() {
        var list = legendLayout.getChildren();
        list.clear();
        for (var legend : handle.legends) {
            var block = new Rectangle(16, 16);
            block.setFill(legend.color());

            list.addAll(block, new Label(legend.name()));
        }
    }

    /*==================*
     * blueprint handle *
     *==================*/

    private Blueprint<Object> blueprint;

    private void updateCategories() {
        handle.resetCategories();
        if (painter == null) return;
        painter.changeFeature(handle);
        updateLegends();
        updateBlueprint();
    }

    @Override
    public void onProbeUpdate(Object chmap, List<ElectrodeDescription> blueprint) {
        var channelmapUpdated = false;
        if (this.blueprint == null || this.blueprint.sameChannelmapCode(chmap)) {
            this.blueprint = new Blueprint<>(probe, chmap, blueprint);
            channelmapUpdated = true;
        } else {
            this.blueprint = new Blueprint<>(this.blueprint, chmap);
            this.blueprint.from(blueprint);
        }

        handle.setChannelmap(chmap);
        if (painter != null && channelmapUpdated) {
            painter.changeChannelmap(handle);
        }

        updateBlueprint();
    }

    private void updateBlueprint() {
        var blueprint = this.blueprint;
        if (!visible.get() || painter == null || blueprint == null) return;

        var display = new Blueprint<>(blueprint);
        display.from(blueprint);

        handle.setBlueprint(display);
        painter.plotBlueprint(handle);

        foreground.clearGraphics();

        var tool = new BlueprintToolkit<>(display);
        for (var legend : handle.legends) {
            var path = categories.computeIfAbsent(legend.name(), _ -> new XYPath());
            foreground.addGraphics(path);

            path.alpha(alphaProperty.get());
            path.fill(legend.color());

            path.clearData();
            for (var clustering : tool.getClusteringEdges(legend.category())) {
                var transform = handle.transform;
                if (transform != null) {
                    var shank = clustering.shank();
                    clustering = clustering.map(corner -> {
                        var x = transform.applyAsDouble(shank, corner.x());
                        return new ClusteringEdges.Corner(x, corner.y(), corner.corner());
                    });
                }

                clustering = clustering.offset(handle.x, handle.y).setCorner(handle.w, handle.h);

                clustering.edges().forEach(c -> path.addData(c.x(), c.y()));

                var c = clustering.edges().get(0);
                path.addData(c.x(), c.y());
                path.addGap();
            }
        }

        foreground.repaint();
    }
}
