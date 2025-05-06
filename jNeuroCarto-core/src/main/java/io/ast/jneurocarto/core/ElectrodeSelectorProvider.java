package io.ast.jneurocarto.core;

import java.util.List;

public interface ElectrodeSelectorProvider {

    List<String> name(ProbeDescription<?> desp);

    ElectrodeSelector newSelector(String name);
}
