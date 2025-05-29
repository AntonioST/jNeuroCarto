package io.ast.jneurocarto.probe_npx.javafx;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.javafx.view.Provide;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

@NullMarked
@RequestChannelmap(probe = NpxProbeDescription.class)
@Provide(ProbeReferencePlugin.class)
@Provide(value = NpxProbeInfoPlugin.class, name = {"probe_npx.info", "neurocarto.views.view_efficient:ElectrodeEfficiencyData"})
@Provide(value = ElectrodeDensityPlugin.class, name = {"probe_npx.density", "neurocarto.views.data_density:ElectrodeDensityDataView"})
@Provide(value = DataVisualizePlugin.class, name = {"probe_npx.data"})
public class NpxProbePluginProvider implements PluginProvider {

    @Override
    public String description() {
        return "Neuropixels probe related plugins";
    }
}
