package io.ast.jneurocarto.probe_npx.javafx;

import javafx.scene.Node;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

public class BadChannelRecorderPlugin implements ProbePlugin<ChannelMap> {

    private final CartoConfig config;
    private final NpxProbeDescription probe;

    public BadChannelRecorderPlugin(CartoConfig config, NpxProbeDescription probe) {
        this.config = config;
        this.probe = probe;
    }

    @Override
    public String description() {
        return "Bad channel recorder";
    }

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        return null;
    }
}
