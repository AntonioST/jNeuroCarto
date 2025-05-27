package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

@NullMarked
public class DataVisualizePluginProvider implements PluginProvider {
    @Override
    public List<String> name() {
        return List.of("probe_npx.data");
    }

    @Override
    public String description() {
        return "show experimental data beside probe";
    }

    @Override
    public @Nullable DataVisualizePlugin setup(CartoConfig config, ProbeDescription<?> desp) {
        if (desp instanceof NpxProbeDescription) {
            return new DataVisualizePlugin(config);
        } else {
            return null;
        }
    }
}
