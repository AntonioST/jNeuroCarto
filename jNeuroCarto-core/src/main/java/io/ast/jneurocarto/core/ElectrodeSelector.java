package io.ast.jneurocarto.core;

import java.util.List;

public interface ElectrodeSelector<D extends ProbeDescription<T>, T> {

    T select(D desp, T chmap, List<ElectrodeDescription> electrodes);
}
