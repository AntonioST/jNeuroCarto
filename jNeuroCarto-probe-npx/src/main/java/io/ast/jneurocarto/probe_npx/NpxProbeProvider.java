package io.ast.jneurocarto.probe_npx;

import io.ast.jneurocarto.core.ProbeProvider;

@SuppressWarnings("unused")
public class NpxProbeProvider implements ProbeProvider {
    @Override
    public String provideProbeFamily() {
        return "npx";
    }

    @Override
    public NpxProbeDescription getProbeDescription() {
        return new NpxProbeDescription();
    }
}
