package io.ast.jneurocarto.probe_npx.select;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

@NullMarked
public class ElectrodeSelectorProvider implements io.ast.jneurocarto.core.ElectrodeSelectorProvider {
    @Override
    public List<String> name(ProbeDescription<?> desp) {
        if (!(desp instanceof NpxProbeDescription)) {
            return List.of();
        }
        return List.of("default", "weaker");
    }

    @Override
    public <D extends ProbeDescription<?>> ElectrodeSelector<D, ?> newSelector(String name) {
        return (ElectrodeSelector<D, ?>) switch (name) {
            case "default" -> new DefaultElectrodeSelector();
            case "weaker" -> new WeakerElectrodeSelector();
            default -> throw new IllegalArgumentException("illegal selector : " + name);
        };
    }
}
