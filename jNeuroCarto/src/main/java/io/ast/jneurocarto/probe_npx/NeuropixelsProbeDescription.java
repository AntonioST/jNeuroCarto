package io.ast.jneurocarto.probe_npx;

import io.ast.jneurocarto.ProbeDescription;

public class NeuropixelsProbeDescription implements ProbeDescription<Object> {
    @Override
    public Class<NpxProbeType> supportedProbeType() {
        return NpxProbeType.class;
    }
}
