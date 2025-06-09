package io.ast.jneurocarto.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.github.classgraph.AnnotationClassRef;
import io.github.classgraph.AnnotationInfo;

/**
 * A record class represent the annotation {@link RequestChannelmap}.
 *
 * @param probe  the class of the request probe description.
 * @param code   the channelmap code of the request channelmap.
 * @param create Create the new probe if it is missing.
 */
@NullMarked
public record RequestChannelmapInfo(Class<? extends ProbeDescription> probe, @Nullable String code, boolean create) {

    /**
     * Create a corresponding requirement with {@link #create}.
     *
     * @param probe the class of the request probe description.
     * @param code  channelmap code
     */
    public RequestChannelmapInfo(Class<? extends ProbeDescription> probe, @Nullable String code) {
        this(probe, code, true);
    }

    /**
     * Create a corresponding requirement.
     *
     * @param family probe family name
     * @return the request information. {@code null} if probe class resolution fail.
     */
    public static @Nullable RequestChannelmapInfo of(String family) {
        if (family.isEmpty()) return null;
        var ret = ProbeDescription.getProbeDescription(family);
        if (ret == null) return null;
        var probe = ret.getClass();
        return new RequestChannelmapInfo(probe, null);
    }

    /**
     * Create a corresponding requirement.
     *
     * @param family probe family name
     * @param code   channelmap code
     * @return the request information. {@code null} if probe class resolution fail.
     */
    public static @Nullable RequestChannelmapInfo of(String family, @Nullable String code) {
        if (family.isEmpty()) return null;
        var ret = ProbeDescription.getProbeDescription(family);
        if (ret == null) return null;
        if (code != null && ret.channelmapCode(code) == null) return null;
        var probe = ret.getClass();
        return new RequestChannelmapInfo(probe, code);
    }

    /**
     * Create a corresponding request information.
     *
     * @param ann a scanned annotation information
     * @return the request information. {@code null} if probe class resolution fail.
     * @throws IllegalArgumentException {@code ann} does not represent {@link RequestChannelmap}
     * @throws RuntimeException         {@code ann} values have wrong type.
     */
    public static @Nullable RequestChannelmapInfo of(AnnotationInfo ann) {
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
                    return null;
                }
                probe = ret.getClass();
            }
        }

        var code = (String) check.getValue("code");
        var create = (boolean) check.getValue("create");

        return new RequestChannelmapInfo(probe, code, create);
    }

    /**
     * Create a corresponding request information.
     *
     * @param check the annotation
     * @return the request information. {@code null} if probe class resolution fail.
     */
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

    /**
     * Create a corresponding request information chain.
     *
     * @param checks the annotation chain. The latter request complete the former.
     * @return the request information. {@code null} if probe class resolution fail.
     */
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

    /**
     * Change the request to always create.
     *
     * @return a new request information.
     */
    public RequestChannelmapInfo alwaysCreate() {
        return new RequestChannelmapInfo(probe, code, true);
    }

    /**
     * Does the {@code probe} fit the request?
     *
     * @param probe testing probe.
     * @return fit
     */
    public boolean checkProbe(ProbeDescription<?> probe) {
        return this.probe.isInstance(probe);
    }

    /**
     * Does the {@code probe} fit the request?
     *
     * @param probe testing probe.
     * @return fit
     */
    public boolean checkProbe(Class<?> probe) {
        return this.probe.isAssignableFrom(probe);
    }

    /**
     * Do the {@code probe} and the {@code channelmap} fit the request?
     *
     * @param probe      testing probe.
     * @param channelmap testing channelmap.
     * @return fit
     */
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
