package io.ast.jneurocarto.core;

import java.util.List;

public interface ElectrodeSelectorProvider {

    List<String> name(ProbeDescription<?> desp);

    <D extends ProbeDescription<?>>
    ElectrodeSelector<D, ?> newSelector(String name);
}
