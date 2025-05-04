package io.ast.jneurocarto.core;

import java.util.List;

public class RandomElectrodeSelector<D extends ProbeDescription<T>, T> implements ElectrodeSelector<D, T> {
    @Override
    public T select(D desp, T chmap, List<ElectrodeDescription> electrodes) {
        return null;
    }
}
