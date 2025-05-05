package io.ast.jneurocarto.core;

import java.util.List;

public class RandomElectrodeSelectorProvider implements ElectrodeSelectorProvider {
    @Override
    public List<String> name(ProbeDescription<?> desp) {
        return List.of("random");
    }

    @Override
    public <D extends ProbeDescription<?>> ElectrodeSelector<D, ?> newSelector(String name) {
        if (!name.equals("random")) {
            throw new IllegalArgumentException();
        }
        return (ElectrodeSelector<D, ?>) new RandomElectrodeSelector();
    }
}
