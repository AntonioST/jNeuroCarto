package io.ast.jneurocarto.javafx.app;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.javafx.script.CheckProbe;

@NullMarked
public record RequestChannelmapType(Class<? extends ProbeDescription> probe, @Nullable String code, boolean create) {
    public RequestChannelmapType(Class<? extends ProbeDescription> probe, @Nullable String code) {
        this(probe, code, true);
    }

    public static @Nullable RequestChannelmapType of(String family) {
        if (family.isEmpty()) return null;
        var ret = ProbeDescription.getProbeDescription(family);
        if (ret == null) return null;
        var probe = ret.getClass();
        return new RequestChannelmapType(probe, null);
    }

    public static @Nullable RequestChannelmapType of(String family, @Nullable String code) {
        if (family.isEmpty()) return null;
        var ret = ProbeDescription.getProbeDescription(family);
        if (ret == null) return null;
        if (code != null && ret.channelmapCode(code) == null) return null;
        var probe = ret.getClass();
        return new RequestChannelmapType(probe, code);
    }

    public static @Nullable RequestChannelmapType of(CheckProbe check) {
        String family = check.value();
        Class<? extends ProbeDescription> probe = check.probe();
        String code = check.code();
        boolean create = check.create();

        if (probe == ProbeDescription.class) {
            if (family.isEmpty()) return null;
            var ret = ProbeDescription.getProbeDescription(family);
            if (ret == null) return null;
            probe = ret.getClass();
        }

        if (code.isEmpty()) code = null;

        return new RequestChannelmapType(probe, code, create);
    }

    public static @Nullable RequestChannelmapType of(@Nullable CheckProbe... checks) {
        if (checks.length == 0) return null;

        String family = "";
        Class<? extends ProbeDescription> probe = ProbeDescription.class;
        String code = "";
        boolean create = false;

        for (var check : checks) {
            if (check != null) {
                if (probe == ProbeDescription.class) probe = check.probe();
                if (family.isEmpty()) family = check.value();
                if (code.isEmpty()) code = check.code();
                create = check.create(); // take last one
            }
        }

        if (probe == ProbeDescription.class) {
            if (family.isEmpty()) return null;
            var ret = ProbeDescription.getProbeDescription(family);
            if (ret == null) return null;
            probe = ret.getClass();
        }

        if (code.isEmpty()) code = null;

        return new RequestChannelmapType(probe, code, create);
    }

    public RequestChannelmapType alwaysCreate() {
        return new RequestChannelmapType(probe, code, true);
    }

    public boolean checkProbe(ProbeDescription<?> probe) {
        return this.probe.isInstance(probe);
    }

    public boolean checkChannelmap(ProbeDescription<?> probe, @Nullable Object channelmap) {
        if (!checkProbe(probe)) return false;
        if (channelmap == null) return true;
        var code = probe.channelmapCode(channelmap);
        if (code == null) return false;
        if (this.code == null) return true;
        return this.code.equals(code);
    }

    @Override
    public String toString() {
        return "RequestChannelmapType[" + probe.getSimpleName() + (code == null ? "" : ":" + code) + "]";
    }
}
