package io.ast.jneurocarto.javafx.view;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ElectrodeDescription;

@NullMarked
public interface ProbePlugin<T> extends Plugin {

    default String description() {
        return name();
    }

    // TODO how do I need to handle clear chmap case?
    default void onProbeUpdate(T chmap, List<ElectrodeDescription> blueprint) {
    }

}
