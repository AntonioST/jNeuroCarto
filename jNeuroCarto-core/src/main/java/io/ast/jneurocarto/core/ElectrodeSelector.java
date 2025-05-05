package io.ast.jneurocarto.core;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ElectrodeSelector<D extends ProbeDescription<T>, T> {

    default Map<String, String> getOptions() {
        return Map.of();
    }

    default void setOptions(Map<String, String> options) {
        options.forEach(this::setOption);
    }

    default void setOption(String name, String value) {
    }

    T select(D desp, T chmap, List<ElectrodeDescription> blueprint);
}
