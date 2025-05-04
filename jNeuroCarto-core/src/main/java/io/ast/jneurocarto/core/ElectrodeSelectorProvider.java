package io.ast.jneurocarto.core;

import java.util.List;

public interface ElectrodeSelectorProvider {

    List<String> name();

    <D extends ProbeDescription<?>>
    ElectrodeSelector<D, ?> newSelector(String name, ProbeDescription<?> desp);
}
