package io.ast.jneurocarto.probe_npx.select;

import java.util.List;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.NeuropixelsProbeDescription;

public class DefaultElectrodeSelector implements ElectrodeSelector<NeuropixelsProbeDescription, ChannelMap> {
    @Override
    public ChannelMap select(NeuropixelsProbeDescription desp, ChannelMap chmap, List<ElectrodeDescription> electrodes) {
        return null;
    }
}
