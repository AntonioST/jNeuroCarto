package io.ast.jneurocarto.probe_npx;

import io.ast.jneurocarto.ProbeProvider;

@SuppressWarnings("unused")
public class NeuropixelsProbeProvider implements ProbeProvider {
    @Override
    public String provideProbeFamily() {
        return "npx";
    }

    @Override
    public NeuropixelsProbeDescription getProbeDescription() {
        return new NeuropixelsProbeDescription();
    }
}
