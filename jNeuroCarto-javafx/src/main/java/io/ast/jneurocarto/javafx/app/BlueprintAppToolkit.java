package io.ast.jneurocarto.javafx.app;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkitWrapper;

@NullMarked
public class BlueprintAppToolkit<T> extends BlueprintToolkitWrapper<T> {
    private final Application<T> application;

    public BlueprintAppToolkit(Application<T> application) {
        var probe = application.probe;
        var chmap = application.view.getChannelmap();
        Blueprint<T> blueprint;
        if (chmap == null) {
            blueprint = new Blueprint<>(probe);
        } else {
            blueprint = new Blueprint<>(probe, chmap);
            blueprint.setBlueprint(Objects.requireNonNull(application.view.getBlueprint(), "null blueprint"));
        }
        this(application, blueprint);
    }

    public BlueprintAppToolkit(Application<T> application, Blueprint<T> blueprint) {
        this(application, new BlueprintToolkit<>(blueprint));
    }

    public BlueprintAppToolkit(Application<T> application, BlueprintToolkit<T> blueprint) {
        super(blueprint);
        this.application = application;
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public BlueprintAppToolkit<T> clone() {
        return new BlueprintAppToolkit<>(application, toolkit.clone());
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
        return checkProbe(probe, null);
    }

    public final boolean checkProbe(String probe, @Nullable String code) {
        ProbeDescription<T> found = (ProbeDescription<T>) ProbeDescription.getProbeDescription(probe);
        if (found == null) throw new RuntimeException("probe " + probe + " not found.");
        return checkProbe((Class<ProbeDescription<T>>) found.getClass(), code);
    }

    public final <P> boolean checkProbe(Class<ProbeDescription<P>> probe, @Nullable String code) {
        var used = probe();
        if (!probe.isInstance(used)) return false;
        if (code == null) return true;
        var chmap = channelmap();
        if (chmap == null) return false;
        return Objects.equals(used.channelmapCode(chmap), code);
    }

    public final void ensureProbe(String probe) {
        ProbeDescription<T> found = (ProbeDescription<T>) ProbeDescription.getProbeDescription(probe);
        if (found == null) throw new RuntimeException("probe " + probe + " not found.");
        ensureProbe((Class<ProbeDescription<T>>) found.getClass());
    }

    public final <P> BlueprintAppToolkit<P> ensureProbe(Class<ProbeDescription<P>> probe) {
        if (!checkProbe(probe)) {
            throw new RequestChannelmapTypeException(new RequestChannelmapType<P>(probe, null));
        }
        return (BlueprintAppToolkit<P>) this;
    }

    public final void ensureProbe(String probe, @Nullable String code) {
        ProbeDescription<T> found = (ProbeDescription<T>) ProbeDescription.getProbeDescription(probe);
        if (found == null) throw new RuntimeException("probe " + probe + " not found.");
        ensureProbe((Class<ProbeDescription<T>>) found.getClass(), code);
    }

    public final <P> BlueprintAppToolkit<P> ensureProbe(Class<ProbeDescription<P>> probe, @Nullable String code) {
        if (!checkProbe(probe, code)) {
            throw new RequestChannelmapTypeException(new RequestChannelmapType<P>(probe, code));
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

    public boolean isCurrentChannelmapUsedByApplication() {
        var chmap = channelmap();
        if (chmap == null) return false;
        return application.view.getChannelmap() == chmap;
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
        switch (mode) {
        case replace -> {
            application.view.clearCaptured();
            application.view.setCaptured(pick(index));
        }
        case append -> application.view.setCaptured(pick(index));
        case exclude -> application.view.unsetCaptured(pick(index));
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
        if (isCurrentChannelmapUsedByApplication()) {
            syncBlueprint();
        }
    }

    public void refreshElectrodeSelection() {
        application.refreshSelection();
        if (isCurrentChannelmapUsedByApplication()) {
            syncBlueprint();
        }
    }

    public void refreshElectrodeSelection(String selector) {
        application.refreshSelection(selector);
        if (isCurrentChannelmapUsedByApplication()) {
            syncBlueprint();
        }
    }

    /*=============*
     * atlas brain *
     *=============*/

    /*====================*
     * probe coordination *
     *====================*/

    /*==========*
     * plotting *
     *==========*/
}
