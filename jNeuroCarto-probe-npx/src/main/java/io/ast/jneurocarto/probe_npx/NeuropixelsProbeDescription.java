package io.ast.jneurocarto.probe_npx;

import io.ast.jneurocarto.core.ProbeDescription;

public class NeuropixelsProbeDescription implements ProbeDescription<Object> {
    @Override
    public Class<NpxProbeType> supportedProbeType() {
        return NpxProbeType.class;
    }
}
