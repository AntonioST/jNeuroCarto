package io.ast.jneurocarto.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ElectrodeSelector {

    /**
     * {@return get option map}.
     */
    default Map<String, String> getOptions() {
        return Map.of();
    }

    /**
     * set options by a map.
     *
     * @param options option map
     */
    default void setOptions(Map<String, String> options) {
        options.forEach(this::setOption);
    }

    /**
     * set option with a string value.
     *
     * @param name  option name
     * @param value option value
     */
    default void setOption(String name, String value) {
    }

    <T> T select(ProbeDescription<T> desp, T chmap, List<ElectrodeDescription> blueprint);

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Selector {
        String value();
    }
}
