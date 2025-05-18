package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.ProbePluginProvider;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

@NullMarked
public class NpxProbePluginProvider implements ProbePluginProvider {
    @Override
    public List<ProbePlugin<?>> setup(CartoConfig config, ProbeDescription<?> desp) {
        System.out.println("NpxProbePluginProvider");
        System.out.println(desp);
        if (desp instanceof NpxProbeDescription probe) {
            var ref = new ProbeReferencePlugin(config, probe);
            return List.of(ref);
        } else {
            return List.of();
        }
    }
}
