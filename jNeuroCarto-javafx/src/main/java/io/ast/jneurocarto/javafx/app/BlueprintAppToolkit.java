package io.ast.jneurocarto.javafx.app;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;

@NullMarked
public class BlueprintAppToolkit<T> extends BlueprintToolkit<T> {
    private final Application<T> application;

    public BlueprintAppToolkit(Application<T> application) {
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

    public BlueprintAppToolkit(Application<T> application, Blueprint<T> blueprint) {
        super(blueprint);
        this.application = application;
    }


    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public BlueprintAppToolkit<T> clone() {
        return new BlueprintAppToolkit<>(application, new Blueprint<>(blueprint));
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
            throw new RequestChannelmapTypeException(new RequestChannelmapType(probe, null));
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
            throw new RequestChannelmapTypeException(new RequestChannelmapType(probe, code));
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
    }

    public void refreshElectrodeSelection() {
        application.refreshSelection();
    }

    public void refreshElectrodeSelection(String selector) {
        application.refreshSelection(selector);
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
