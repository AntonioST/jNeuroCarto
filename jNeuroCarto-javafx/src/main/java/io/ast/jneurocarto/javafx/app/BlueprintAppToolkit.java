package io.ast.jneurocarto.javafx.app;

import java.util.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.atlas.ImageSliceStack;
import io.ast.jneurocarto.atlas.SliceCoordinate;
import io.ast.jneurocarto.atlas.Structure;
import io.ast.jneurocarto.core.*;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.atlas.AtlasPlugin;
import io.ast.jneurocarto.javafx.atlas.CoordinateLabel;
import io.ast.jneurocarto.javafx.script.ScriptPlugin;
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

    public void setCaptureElectrodes(int[] index, CaptureMode mode) {
        var view = application.view;
        var electrodes = view.getBlueprint();
        if (electrodes == null) return;

        switch (mode) {
        case replace -> {
            view.clearCaptured();
            view.setCaptured(pick(electrodes, index));
        }
        case append -> view.setCaptured(pick(electrodes, index));
        case exclude -> view.unsetCaptured(pick(electrodes, index));
        }
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

    public <P extends Plugin> Optional<P> getPlugin(Class<P> cls) {
        return Optional.ofNullable(application.getPlugin(cls));
    }

    /*==================*
     * blueprint script *
     *==================*/

    public boolean hasScript(String name) {
        return getPlugin(ScriptPlugin.class).map(p -> p.hasScript(name)).orElse(false);
    }

    public void runScript(String name, String... args) {
        runScript(name, Arrays.asList(args));
    }

    public void runScript(String name, List<String> args) {
        runScript(name, args, Map.of());
    }

    public void runScript(String name, List<String> args, Map<String, String> kwargs) {
        getPlugin(ScriptPlugin.class).ifPresent(p -> p.runScript(name, args, kwargs));
    }

    public boolean interruptScript(String name) {
        return getPlugin(ScriptPlugin.class).map(p -> p.interruptScript(name)).orElse(false);
    }

    public void setScriptInput(String name, String... args) {
        getPlugin(ScriptPlugin.class).ifPresent(p -> {
            if (p.selectScript(name)) {
                for (var arg : args) {
                    p.appendScriptInputValueText(arg);
                }
            }
        });
    }

    /*=============*
     * atlas brain *
     *=============*/

    public @Nullable String atlasGetRegion(int id) {
        return getPlugin(AtlasPlugin.class)
          .map(p -> p.getRegion(id))
          .map(Structure::name)
          .orElse(null);
    }

    public @Nullable String atlasGetRegion(String name) {
        return getPlugin(AtlasPlugin.class)
          .map(p -> p.getRegion(name))
          .map(Structure::name)
          .orElse(null);
    }

    public @Nullable ImageSlice atlasGetSlice() {
        return getPlugin(AtlasPlugin.class).flatMap(p -> Optional.ofNullable(p.getCurrentSlice())).orElse(null);
    }

    public void atlasSetSlice(ImageSliceStack.Projection projection) {
        getPlugin(AtlasPlugin.class).ifPresent(p -> {
            p.setProjection(projection);
        });
    }

    public void atlasSetSlice(ImageSliceStack.Projection projection, double plane) {
        getPlugin(AtlasPlugin.class).ifPresent(p -> {
            p.setProjection(projection);
            p.setPlane(plane);
        });
    }

    public @Nullable CoordinateLabel atlasGetLabel(String text) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasGetLabel
        throw new UnsupportedOperationException();
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, Coordinate coordinate) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasAddLabel
        throw new UnsupportedOperationException();
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, SliceCoordinate coordinate) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasAddLabel
        throw new UnsupportedOperationException();
    }

    public @Nullable CoordinateLabel atlasAddLabel(String text, double x, double y) {
        var slice = atlasGetSlice();
        if (slice == null) return null;
        return atlasAddLabel(text, new SliceCoordinate(slice.plane(), x, y));
    }

    public void atlasFocusLabel(String text) {
        var label = atlasGetLabel(text);
        if (label != null) atlasFocusLabel(label);
    }

    public void atlasFocusLabel(CoordinateLabel label) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasFocusLabel
        throw new UnsupportedOperationException();
    }

    public void atlasRemoveLabel(String text) {
        var label = atlasGetLabel(text);
        if (label != null) atlasRemoveLabel(label);
    }

    public void atlasRemoveLabel(CoordinateLabel label) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasRemoveLabel
        throw new UnsupportedOperationException();
    }

    public void atlasClearLabels() {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasClearLabels
        throw new UnsupportedOperationException();
    }

    /*====================*
     * probe coordination *
     *====================*/

    public void atlasSetTransform(SliceCoordinate coordinate, double rotation) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasSetTransform
        throw new UnsupportedOperationException();
    }

    public void atlasSetAnchor(double x, double y) {
        atlasSetAnchor(x, y, 0, 0);
    }

    public void atlasSetAnchor(double x, double y, double ax, double ay) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasSetAnchor
        throw new UnsupportedOperationException();
    }

    public @Nullable ProbeCoordinate atlasNewProbeCoordinate() {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasNewProbeCoordinate
        throw new UnsupportedOperationException();
    }

    public @Nullable ProbeCoordinate atlasCurrentProbeCoordinate() {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasCurrentProbeCoordinate
        throw new UnsupportedOperationException();
    }

    public void atlasSetAnchorOnProbe(ProbeCoordinate coordinate) {
        //XXX Unsupported Operation BlueprintAppToolkit.atlasCurrentProbeCoordinate
        throw new UnsupportedOperationException();
    }

    /*==========*
     * plotting *
     *==========*/
}
