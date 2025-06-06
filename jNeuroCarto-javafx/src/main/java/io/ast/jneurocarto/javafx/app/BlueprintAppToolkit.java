package io.ast.jneurocarto.javafx.app;

import java.util.*;
import java.util.function.Consumer;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.atlas.ImageSliceStack;
import io.ast.jneurocarto.atlas.SliceCoordinate;
import io.ast.jneurocarto.atlas.Structure;
import io.ast.jneurocarto.core.*;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintMask;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.atlas.*;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.event.DataSelectEvent;
import io.ast.jneurocarto.javafx.script.ScriptPlugin;
import io.ast.jneurocarto.javafx.utils.OnceForget;
import io.ast.jneurocarto.javafx.view.Plugin;

@NullMarked
public class BlueprintAppToolkit<T> extends BlueprintToolkit<T> {
    private final Application<T> application;

    private BlueprintAppToolkit(Application<T> application) {
        var probe = application.probe;
        var chmap = application.view.getChannelmap();
        Blueprint<T> blueprint;
        if (chmap == null) {
            blueprint = new Blueprint<>(probe);
        } else {
            blueprint = new Blueprint<>(probe, chmap);
            blueprint.from(Objects.requireNonNull(application.view.getBlueprint(), "null blueprint"));
        }
        this(application, blueprint);
    }

    private BlueprintAppToolkit(Application<T> application, Blueprint<T> blueprint) {
        super(blueprint);
        this.application = application;
    }


    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public BlueprintAppToolkit<T> clone() {
        return new BlueprintAppToolkit<>(application, new Blueprint<>(blueprint));
    }

    public static <T> BlueprintAppToolkit<T> newToolkit() {
        var app = (Application<T>) Application.getInstance();
        var chmap = app.view.getChannelmap();
        Blueprint<T> blueprint;
        if (chmap == null) {
            blueprint = new Blueprint<>(app.probe);
        } else {
            var electrodes = app.view.getBlueprint();
            blueprint = new Blueprint<>(app.probe, chmap, electrodes);
        }
        return new BlueprintAppToolkit<>(app, blueprint);
    }

    /*=======================*
     * application functions *
     *=======================*/

    public void printLogMessage(String message) {
        application.printMessage(message);
    }

    public void printLogMessage(List<String> message) {
        application.printMessage(message);
    }

    public void clearLogMessage() {
        application.clearMessages();
    }

    /*=======*
     * probe *
     *=======*/

    public final boolean checkProbe(String probe) {
        return checkProbe(probe, null);
    }

    public final <P> boolean checkProbe(Class<ProbeDescription<P>> probe) {
        return checkProbe(new RequestChannelmapInfo(probe, null));
    }

    public final boolean checkProbe(String probe, @Nullable String code) {
        var request = RequestChannelmapInfo.of(probe, code);
        if (request == null) throw new RuntimeException("probe " + probe + " not found.");
        return checkProbe(request);
    }

    public final <P> boolean checkProbe(Class<ProbeDescription<P>> probe, @Nullable String code) {
        return checkProbe(new RequestChannelmapInfo(probe, code));
    }

    public final <P> boolean checkProbe(RequestChannelmapInfo request) {
        return request.checkChannelmap(probe(), channelmap());
    }

    public final void ensureProbe(String probe) {
        ensureProbe(probe, null);
    }

    public final <P> BlueprintAppToolkit<P> ensureProbe(Class<ProbeDescription<P>> probe) {
        return ensureProbe(new RequestChannelmapInfo(probe, null));
    }

    public final void ensureProbe(String probe, @Nullable String code) {
        var request = RequestChannelmapInfo.of(probe, code);
        if (request == null) throw new RuntimeException("probe " + probe + " not found.");
        ensureProbe(request);
    }

    public final <P> BlueprintAppToolkit<P> ensureProbe(Class<ProbeDescription<P>> probe, @Nullable String code) {
        return ensureProbe(new RequestChannelmapInfo(probe, code));
    }

    public final <P> BlueprintAppToolkit<P> ensureProbe(RequestChannelmapInfo request) {
        if (!checkProbe(request)) {
            throw new RequestChannelmapException(request);
        }
        return (BlueprintAppToolkit<P>) this;
    }

    /*============*
     * channelmap *
     *============*/

    public BlueprintAppToolkit<T> newChannelmap(String code) {
        application.clearProbe(code);
        return new BlueprintAppToolkit<>(application);
    }


    public BlueprintAppToolkit<T> setChannelmap(T channelmap) {
        application.clearProbe(channelmap);
        return new BlueprintAppToolkit<>(application);
    }

    public boolean isCurrentChannelmapUsedByApplication() {
        var chmap = channelmap();
        if (chmap == null) return false;
        return application.view.getChannelmap() == chmap;
    }

    public void addElectrode(int[] index) {
        addElectrode(index, 0, index.length);
    }

    public void addElectrode(int[] index, int offset, int length) {
        var probe = probe();
        var chmap = channelmap();
        if (chmap == null) throw new RuntimeException();
        for (var e : pick(index, offset, length)) {
            probe.addElectrode(chmap, e);
        }
    }

    public void removeElectrode(int[] index) {
        removeElectrode(index, 0, index.length);
    }

    public void removeElectrode(int[] index, int offset, int length) {
        var probe = probe();
        var chmap = channelmap();
        if (chmap == null) throw new RuntimeException();
        for (var e : pick(index, offset, length)) {
            probe.removeElectrode(chmap, e);
        }
    }

    /*============*
     * electrodes *
     *============*/

    public enum CaptureMode {
        replace, append, exclude;
    }

    public int[] getCaptureElectrodes() {
        return index(application.view.getCaptured(ProbeDescription.STATE_USED, false));
    }

    public int[] getAllCaptureElectrodes() {
        return index(application.view.getCaptured(false));
    }

    public boolean setCaptureElectrodes(int[] index, CaptureMode mode) {
        var view = application.view;
        var electrodes = view.getBlueprint();
        if (electrodes == null) return false;

        switch (mode) {
        case replace -> {
            view.clearCaptured();
            view.setCaptured(pick(electrodes, index));
        }
        case append -> view.setCaptured(pick(electrodes, index));
        case exclude -> view.unsetCaptured(pick(electrodes, index));
        }
        return true;
    }

    public boolean setCaptureElectrodes(BlueprintMask mask, CaptureMode mode) {
        var view = application.view;
        var electrodes = view.getBlueprint();
        if (electrodes == null) return false;

        switch (mode) {
        case replace -> {
            view.clearCaptured();
            view.setCaptured(pick(electrodes, mask));
        }
        case append -> view.setCaptured(pick(electrodes, mask));
        case exclude -> view.unsetCaptured(pick(electrodes, mask));
        }
        return true;
    }

    public void setCaptureElectrodesInRegion(int region, CaptureMode mode) throws PluginNotLoadException {
        var atlas = getPlugin(AtlasPlugin.class);
        var structure = atlas.getRegion(region);
        if (structure == null) throw new RuntimeException("structure with id " + region + " not found.");
        setCaptureElectrodesInRegion(atlas, structure, mode);
    }

    public void setCaptureElectrodesInRegion(String region, CaptureMode mode) throws PluginNotLoadException {
        var atlas = getPlugin(AtlasPlugin.class);
        var structure = atlas.getRegion(region);
        if (structure == null) throw new RuntimeException("structure with name " + region + " not found.");
        setCaptureElectrodesInRegion(atlas, structure, mode);
    }

    private void setCaptureElectrodesInRegion(AtlasPlugin atlas, Structure structure, CaptureMode mode) {
        var brain = atlas.getBrainAtlas();
        var image = atlas.getImageSlice();
        if (brain == null || image == null) return;

        var mask = mask(e -> {
            var s = brain.structureAt(image.pullBack(e.x(), e.y()));
            return s != null && s.hasParent(structure);
        });
        setCaptureElectrodes(mask, mode);
    }

    public void clearCaptureElectrodes() {
        application.view.clearCaptured();
    }

    public void setStateForCapturedElectrodes(int state) {
        application.view.setStateForCaptured(state);
    }

    public void setCategoryForCapturedElectrodes(int category) {
        application.view.setCategoryForCaptured(category);
    }

    public void syncViewBlueprint() {
        var electrodes = application.view.getBlueprint();
        if (electrodes == null) return;
        from(electrodes);
    }

    public void applyViewBlueprint() {
        var electrodes = application.view.getBlueprint();
        if (electrodes == null) return;
        apply(electrodes);
        application.fireProbeUpdate();
    }

    public void refreshElectrodeSelection() {
        application.refreshSelection();
    }

    public void refreshElectrodeSelection(String selector) {
        application.refreshSelection(selector);
    }

    public void repaintViewBlueprint() {
        application.view.repaint();
    }

    /*========*
     * plugin *
     *========*/

    public Optional<Plugin> getPlugin(String name) {
        return Optional.ofNullable(application.getPlugin(name));
    }

    public <P extends Plugin> P getPlugin(Class<P> cls) throws PluginNotLoadException {
        var ret = application.getPlugin(cls);
        if (ret == null) throw new PluginNotLoadException(cls);
        return ret;
    }

    /*==================*
     * blueprint script *
     *==================*/

    public boolean hasScript(String name) throws PluginNotLoadException {
        return getPlugin(ScriptPlugin.class).hasScript(name);
    }

    public void runScript(String name, String... args) throws PluginNotLoadException {
        runScript(name, Arrays.asList(args));
    }

    public void runScript(String name, List<String> args) throws PluginNotLoadException {
        runScript(name, args, Map.of());
    }

    public void runScript(String name, List<String> args, Map<String, String> kwargs) throws PluginNotLoadException {
        getPlugin(ScriptPlugin.class).runScript(name, args, kwargs);
    }

    public boolean interruptScript(String name) throws PluginNotLoadException {
        return getPlugin(ScriptPlugin.class).interruptScript(name);
    }

    public void setScriptInput(String name, String... args) throws PluginNotLoadException {
        var p = getPlugin(ScriptPlugin.class);
        if (p.selectScript(name)) {
            for (var arg : args) {
                p.appendScriptInputValueText(arg);
            }
        }
    }

    /*=============*
     * atlas brain *
     *=============*/

    public @Nullable String atlasGetRegion(int id) throws PluginNotLoadException {
        var p = getPlugin(AtlasPlugin.class);
        var r = p.getRegion(id);
        return r == null ? null : r.acronym();
    }

    public @Nullable String atlasGetRegion(String name) throws PluginNotLoadException {
        var p = getPlugin(AtlasPlugin.class);
        var r = p.getRegion(name);
        return r == null ? null : r.acronym();
    }

    public ImageSliceStack.@Nullable Projection atlasGetProjection() throws PluginNotLoadException {
        return getPlugin(AtlasPlugin.class).getProjection();
    }

    public @Nullable AtlasReference atlasGetReference() throws PluginNotLoadException {
        return getPlugin(AtlasPlugin.class).getAtlasReference();
    }

    public @Nullable ImageSlice atlasGetSlice() throws PluginNotLoadException {
        return getPlugin(AtlasPlugin.class).getImageSlice();
    }

    public void atlasSetSlice(ImageSliceStack.Projection projection) throws PluginNotLoadException {
        getPlugin(AtlasPlugin.class).setProjection(projection);
    }

    /**
     * @param projection
     * @param plane      plane (um) in referenced anatomical space
     */
    public void atlasSetSlice(ImageSliceStack.Projection projection, double plane) throws PluginNotLoadException {
        var p = getPlugin(AtlasPlugin.class);
        p.setProjection(projection);
        p.setPlane(plane);
    }

    /*======================*
     * Atlas region masking *
     *======================*/

    public AtlasPlugin.RegionMask atlasCreateMask(String name) throws PluginNotLoadException {
        return atlasCreateMask(name, false, true);
    }

    public AtlasPlugin.RegionMask atlasCreateMask(String name, boolean exclude) throws PluginNotLoadException {
        return atlasCreateMask(name, exclude, true);
    }

    public AtlasPlugin.RegionMask atlasCreateMask(String name, boolean exclude, boolean includeChildren) throws PluginNotLoadException {
        var structure = getPlugin(AtlasPlugin.class).getRegion(name);
        if (structure == null) throw new RuntimeException("region " + name + " not existed");
        return new AtlasPlugin.RegionMask(structure, exclude, includeChildren);
    }

    public List<AtlasPlugin.RegionMask> atlasGetMask() throws PluginNotLoadException {
        return getPlugin(AtlasPlugin.class).getMaskedRegions();
    }

    public void atlasSetMask(List<AtlasPlugin.RegionMask> masks) throws PluginNotLoadException {
        getPlugin(AtlasPlugin.class).setMaskedRegions(masks);
    }

    public void atlasAddMask(AtlasPlugin.RegionMask mask) throws PluginNotLoadException {
        getPlugin(AtlasPlugin.class).addMaskedRegion(mask);
    }

    public void atlasClearMasks() throws PluginNotLoadException {
        getPlugin(AtlasPlugin.class).clearMaskedRegions();
    }

    /*=============*
     * Atlas label *
     *=============*/

    public @Nullable CoordinateLabel atlasGetLabel(String text) throws PluginNotLoadException {
        return getPlugin(AtlasLabelPlugin.class).getLabel(text);
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, Coordinate coordinate, String color) throws PluginNotLoadException {
        var p = getPlugin(AtlasLabelPlugin.class);
        var label = new CoordinateLabel(text, new CoordinateLabel.AtlasPosition(coordinate, null), color);
        p.addLabel(label);
        return label;
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, String reference, Coordinate coordinate, String color) throws PluginNotLoadException {
        var p = getPlugin(AtlasLabelPlugin.class);
        var label = new CoordinateLabel(text, new CoordinateLabel.AtlasPosition(coordinate, reference), color);
        p.addLabel(label);
        return label;
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, SliceCoordinate coordinate, String color) throws PluginNotLoadException {
        var projection = getPlugin(AtlasPlugin.class).getProjection();
        return atlasAddLabel(text, projection, coordinate, color);
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, ImageSliceStack.Projection projection, SliceCoordinate coordinate, String color) throws PluginNotLoadException {
        var p = getPlugin(AtlasLabelPlugin.class);
        var label = new CoordinateLabel(text, new CoordinateLabel.SlicePosition(projection, coordinate), color);
        p.addLabel(label);
        return label;
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, ProbeCoordinate coordinate, String color) throws PluginNotLoadException {
        var p = getPlugin(AtlasLabelPlugin.class);
        var label = new CoordinateLabel(text, new CoordinateLabel.ProbePosition(coordinate), color);
        p.addLabel(label);
        return label;
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, double x, double y, String color) throws PluginNotLoadException {
        var p = getPlugin(AtlasLabelPlugin.class);
        var label = new CoordinateLabel(text, new CoordinateLabel.ProbePosition(new ProbeCoordinate(0, x, y)), color);
        p.addLabel(label);
        return label;
    }

    public void atlasFocusLabel(String text) throws PluginNotLoadException {
        var label = atlasGetLabel(text);
        if (label != null) atlasFocusLabel(label);
    }

    public void atlasFocusLabel(@Nullable CoordinateLabel label) throws PluginNotLoadException {
        if (label != null) {
            getPlugin(AtlasLabelPlugin.class).focusOnLabel(label);
        }
    }

    public boolean atlasIsLabelVisible(String text) throws PluginNotLoadException {
        var label = atlasGetLabel(text);
        return label != null && atlasIsLabelVisible(label);
    }

    public boolean atlasIsLabelVisible(@Nullable CoordinateLabel label) throws PluginNotLoadException {
        return label != null && getPlugin(AtlasLabelPlugin.class).isVisible(label);
    }

    public void atlasSetLabelVisible(String text, boolean visible) throws PluginNotLoadException {
        var label = atlasGetLabel(text);
        if (label != null) atlasSetLabelVisible(label, visible);
    }

    public void atlasSetLabelVisible(@Nullable CoordinateLabel label, boolean visible) throws PluginNotLoadException {
        if (label != null) {
            getPlugin(AtlasLabelPlugin.class).setVisible(label, visible);
        }
    }

    public void atlasRemoveLabel(String text) throws PluginNotLoadException {
        var label = atlasGetLabel(text);
        if (label != null) atlasRemoveLabel(label);
    }

    public void atlasRemoveLabel(@Nullable CoordinateLabel label) throws PluginNotLoadException {
        if (label == null) return;
        getPlugin(AtlasLabelPlugin.class).removeLabel(label);
    }

    public void atlasClearLabels() throws PluginNotLoadException {
        getPlugin(AtlasLabelPlugin.class).clearLabels();
    }

    /*====================*
     * probe coordination *
     *====================*/

    /**
     * Move atlas image coordinate {@code (x, y)} to chart origin.
     *
     * @param x slice coordinate
     * @param y slice coordinate
     */
    public void atlasSetAnchor(double x, double y) throws PluginNotLoadException {
        atlasSetAnchor(x, y, 0, 0);
    }

    /**
     * Move the slice to make the {@code coordinate} on chart point {@code p},
     * but ignore plane adjusting.
     *
     * @param x  slice coordinate
     * @param y  slice coordinate
     * @param ax chart position
     * @param ay chart position
     */
    public void atlasSetAnchor(double x, double y, double ax, double ay) throws PluginNotLoadException {
        getPlugin(AtlasPlugin.class).anchorImageTo(new SliceCoordinate(Double.NaN, x, y), new Point2D(ax, ay));
    }

    /**
     * Move the slice to make the {@code coordinate} on chart point {@code p}.
     *
     * @param coordinate slice coordinate, {@link SliceCoordinate#p} follow current atlas reference.
     *                   If it is {@link Double#NaN}, then skip plane adjusting.
     * @param ax         chart position
     * @param ay         chart position
     */
    public void atlasSetAnchor(SliceCoordinate coordinate, double ax, double ay) throws PluginNotLoadException {
        getPlugin(AtlasPlugin.class).anchorImageTo(coordinate, new Point2D(ax, ay));
    }

    public @Nullable ImplantCoordinate atlasCurrentProbeCoordinate() throws PluginNotLoadException {
        return getPlugin(ImplantPlugin.class).getImplantCoordinate();
    }

    public @Nullable ImplantCoordinate atlasNewProbeCoordinate() throws PluginNotLoadException {
        return atlasNewProbeCoordinate(0);
    }

    public @Nullable ImplantCoordinate atlasNewProbeCoordinate(int shank) throws PluginNotLoadException {
        return getPlugin(ImplantPlugin.class).newImplantCoordinate(shank);
    }

    public void atlasSetAnchorOnProbe(ImplantCoordinate coordinate) throws PluginNotLoadException {
        getPlugin(ImplantPlugin.class).focusImplantCoordinate(coordinate);
    }

    /*==========*
     * plotting *
     *==========*/

    public void repaintForeground() {
        application.view.repaintForeground();
    }

    public void repaintBackground() {
        application.view.repaintBackground();
    }

    public OnceForget listenOnSelect(EventHandler<DataSelectEvent> handle) {
        var ret = new OnceForget(() -> application.view.removeEventHandler(DataSelectEvent.DATA_SELECT, handle));
        application.view.addEventHandler(DataSelectEvent.DATA_SELECT, handle);
        return ret;
    }

    public OnceForget paintOnForeground(Consumer<GraphicsContext> plotting) {
        var view = application.view;
        var painter = new InteractionXYChart.PlottingJob() {
            @Override
            public void draw(GraphicsContext gc) {
                gc.setTransform(view.getCanvasTransform());
                plotting.accept(gc);
            }
        };
        var ret = new OnceForget(() -> view.removeForegroundPlotting(painter));
        view.addForegroundPlotting(painter);
        return ret;
    }

    public OnceForget paintOnBackground(Consumer<GraphicsContext> plotting) {
        var view = application.view;
        var painter = new InteractionXYChart.PlottingJob() {
            @Override
            public void draw(GraphicsContext gc) {
                gc.setTransform(view.getCanvasTransform());
                plotting.accept(gc);
            }
        };
        var ret = new OnceForget(() -> view.removeBackgroundPlotting(painter));
        view.addBackgroundPlotting(painter);
        return ret;
    }
}
