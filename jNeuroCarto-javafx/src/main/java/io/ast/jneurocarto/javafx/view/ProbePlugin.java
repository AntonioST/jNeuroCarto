package io.ast.jneurocarto.javafx.view;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ElectrodeDescription;

@NullMarked
public interface ProbePlugin<T> extends Plugin {

    default void onProbeUpdate(T chmap, List<ElectrodeDescription> blueprint) {
    }

}
