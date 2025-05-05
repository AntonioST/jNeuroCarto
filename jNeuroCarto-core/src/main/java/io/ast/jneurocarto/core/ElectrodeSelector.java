package io.ast.jneurocarto.core;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ElectrodeSelector<D extends ProbeDescription<T>, T> {

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

    T select(D desp, T chmap, List<ElectrodeDescription> blueprint);
}
