package io.ast.jneurocarto.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.AnnotationInfo;

@NullMarked
public record RequestChannelmapInfo(Class<? extends ProbeDescription> probe, @Nullable String code, boolean create) {
    public RequestChannelmapInfo(Class<? extends ProbeDescription> probe, @Nullable String code) {
        this(probe, code, true);
    }

    public static @Nullable RequestChannelmapInfo of(String family) {
        if (family.isEmpty()) return null;
        var ret = ProbeDescription.getProbeDescription(family);
        if (ret == null) return null;
        var probe = ret.getClass();
        return new RequestChannelmapInfo(probe, null);
    }

    public static @Nullable RequestChannelmapInfo of(String family, @Nullable String code) {
        if (family.isEmpty()) return null;
        var ret = ProbeDescription.getProbeDescription(family);
        if (ret == null) return null;
        if (code != null && ret.channelmapCode(code) == null) return null;
        var probe = ret.getClass();
        return new RequestChannelmapInfo(probe, code);
    }

    public static RequestChannelmapInfo of(AnnotationInfo ann) {
        if (!RequestChannelmap.class.getName().equals(ann.getClassInfo().getName())) {
            throw new IllegalArgumentException("not @RequestChannelmap AnnotationInfo");
        }
        var check = ann.getParameterValues();
        var family = (String) check.getValue("value");

        var probeValue = check.getValue("probe");
        Class<? extends ProbeDescription> probe;
        if (probeValue instanceof AnnotationClassRef ref) {
            var refClass = ref.loadClass(true);
            if (refClass == null) {
                throw new RuntimeException("unable to load probe : " + probeValue);
            } else if (ProbeDescription.class.isAssignableFrom(refClass)) {
                probe = (Class<? extends ProbeDescription>) refClass;
            } else {
                throw new RuntimeException(refClass.getName() + " not a ProbeDescription");
            }
        } else {
            throw new RuntimeException("unknown probe() value : " + probeValue);
        }

        if (probe == ProbeDescription.class) {
            if (!family.isEmpty()) {
                var ret = ProbeDescription.getProbeDescription(family);
                if (ret == null) {
                    throw new RuntimeException("unable to load probe family : " + family);
                }
                probe = ret.getClass();
            }
        }

        var code = (String) check.getValue("code");
        var create = (boolean) check.getValue("create");

        return new RequestChannelmapInfo(probe, code, create);
    }

    public static @Nullable RequestChannelmapInfo of(RequestChannelmap check) {
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

        return new RequestChannelmapInfo(probe, code, create);
    }

    public static @Nullable RequestChannelmapInfo of(@Nullable RequestChannelmap... checks) {
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

        return new RequestChannelmapInfo(probe, code, create);
    }

    public RequestChannelmapInfo alwaysCreate() {
        return new RequestChannelmapInfo(probe, code, true);
    }

    public boolean checkProbe(ProbeDescription<?> probe) {
        return this.probe.isInstance(probe);
    }

    public boolean checkProbe(Class<?> probe) {
        return this.probe.isAssignableFrom(probe);
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
