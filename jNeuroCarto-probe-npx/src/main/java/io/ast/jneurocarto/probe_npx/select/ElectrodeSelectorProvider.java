package io.ast.jneurocarto.probe_npx.select;

import java.util.List;

import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.probe_npx.NeuropixelsProbeDescription;

public class ElectrodeSelectorProvider implements io.ast.jneurocarto.core.ElectrodeSelectorProvider {
    @Override
    public List<String> name(ProbeDescription<?> desp) {
        if (!(desp instanceof NeuropixelsProbeDescription)) {
            return List.of();
        }
        return List.of("default", "weaker");
    }

    @Override
    public <D extends ProbeDescription<?>> ElectrodeSelector<D, ?> newSelector(String name) {
        return (ElectrodeSelector<D, ?>) switch (name) {
            case "name" -> new DefaultElectrodeSelector();
            case "weaker" -> new WeakerElectrodeSelector();
            default -> throw new IllegalArgumentException();
        };
    }
}
