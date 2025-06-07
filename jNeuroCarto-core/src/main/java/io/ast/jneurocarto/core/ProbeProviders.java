package io.ast.jneurocarto.core;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import io.github.classgraph.ClassGraph;

/**
 * Utility functions for {@link ProbeDescription}.
 */
public final class ProbeProviders {

    /// Found probe descriptions in the classpath.
    ///
    /// @return list of probe descriptions.
    /// @see ProbeDescription#listProbeDescriptions()
    /// @see ProbeProvider
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

    private static final Map<Class<? extends ProbeDescription>, List<SelectorInfo>> SELECTORS = new WeakHashMap<>();

    private record SelectorInfo(String name, @Nullable RequestChannelmapInfo request, Class<ElectrodeSelector> clazz) {
    }

    public static List<String> getElectrodeSelectors(ProbeDescription<?> probe) {
        return SELECTORS.computeIfAbsent(probe.getClass(), ProbeProviders::scanElectrodeSelectors).stream()
          .map(SelectorInfo::name)
          .toList();
    }

    public static ElectrodeSelector newElectrodeSelector(ProbeDescription<?> probe, String name) {
        var info = SELECTORS.computeIfAbsent(probe.getClass(), ProbeProviders::scanElectrodeSelectors).stream()
          .filter(it -> it.name.equals(name))
          .findFirst()
          .orElseThrow(() -> new RuntimeException("select name " + name + " not found"));

        if (info.request != null && !info.request.checkProbe(probe)) {
            throw new RuntimeException("select name " + name + " reject probe " + probe.getClass().getSimpleName());
        }

        try {
            return info.clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("unable to create selector " + info.clazz.getSimpleName(), e);
        }
    }

    private static List<SelectorInfo> scanElectrodeSelectors(Class<? extends ProbeDescription> probe) {
        var log = LoggerFactory.getLogger(ProbeProviders.class);
        log.debug("scanElectrodeSelectors for probe {}", probe.getSimpleName());

        var scan = new ClassGraph()
//          .verbose()
          .enableClassInfo()
          .enableAnnotationInfo()
          .acceptPackages(
            ProbeDescription.class.getPackageName(),
            probe.getPackageName()
          );

        var ret = new ArrayList<SelectorInfo>();
        try (var result = scan.scan()) {
            for (var info : result.getClassesImplementing(ElectrodeSelector.class)) {
                String name = info.getSimpleName();
                var selector = info.getAnnotationInfo(ElectrodeSelector.Selector.class);
                if (selector != null) {
                    name = (String) selector.getParameterValues().getValue("value");
                    log.debug("found selector {} = {}", name, info.getName());
                } else {
                    log.debug("found selector {}", info.getName());
                }


                RequestChannelmapInfo request = null;
                var check = info.getAnnotationInfo(RequestChannelmap.class);
                if (check != null) {
                    try {
                        request = RequestChannelmapInfo.of(check);
                    } catch (Exception e) {
                        log.debug("reject selector {}", e.getMessage());
                        continue;
                    }
                    log.debug("selector {} request {}", name, request.probe().getName());
                    if (!request.checkProbe(probe)) {
                        log.debug("reject selector : probe mismatch");
                        continue;
                    }
                }

                var clazz = (Class<ElectrodeSelector>) info.loadClass(true);

                ret.add(new SelectorInfo(name, request, clazz));
            }
        }
        return ret;
    }


}
