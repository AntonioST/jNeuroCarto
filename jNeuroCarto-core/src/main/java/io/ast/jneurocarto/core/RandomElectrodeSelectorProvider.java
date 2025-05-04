package io.ast.jneurocarto.core;

import java.util.List;

public class RandomElectrodeSelectorProvider implements ElectrodeSelectorProvider {
    @Override
    public List<String> name() {
        return List.of("random");
    }

    @Override
    public <D extends ProbeDescription<?>> ElectrodeSelector<D, ?> newSelector(String name, ProbeDescription<?> desp) {
        if (!name.equals("random")) {
            throw new IllegalArgumentException();
        }
        return new RandomElectrodeSelector();
    }
}
