import io.ast.jneurocarto.core.ProbeProvider;
import io.ast.jneurocarto.probe_npx.NpxProbeProvider;

/**
 * Neuropixels probe general extension.
 */
module io.ast.jneurocarto.probe_npx {

    requires org.slf4j;

    requires static info.picocli;
    requires static org.jspecify;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.probe_npx;
    exports io.ast.jneurocarto.probe_npx.cli;

    provides ProbeProvider with NpxProbeProvider;

    opens io.ast.jneurocarto.probe_npx.cli to info.picocli;
}