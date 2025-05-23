package io.ast.jneurocarto.core;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

public final class ProbeProviders {

    public static List<String> listProbeDescriptions() {
        var ret = new ArrayList<String>();
        for (var provider : ServiceLoader.load(ProbeProvider.class)) {
            ret.add(provider.name());
        }
        return ret;
    }

    public static @Nullable String getProbeDescriptionText(String family) {
        for (var provider : ServiceLoader.load(ProbeProvider.class)) {
            if (provider.name().equals(family)) {
                var ret = provider.description();
                if (ret.isEmpty()) {
                    ret = provider.name();
                }
                return ret;
            }
        }
        return null;
    }

    public static @Nullable ProbeDescription<?> getProbeDescription(String family) {
        for (var provider : ServiceLoader.load(ProbeProvider.class)) {
            if (provider.name().equals(family)) {
                return provider.getProbeDescription();
            }
        }
        return null;
    }

    public static <T> @Nullable ProbeDescription<T> getProbeDescription(Class<ProbeDescription<T>> probe) {
        for (var provider : ServiceLoader.load(ProbeProvider.class)) {
            var ret = provider.getProbeDescription();
            if (probe.isInstance(ret)) {
                return (ProbeDescription<T>) ret;
            }
        }
        return null;
    }
}
