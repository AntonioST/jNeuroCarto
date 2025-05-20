import io.ast.jneurocarto.core.ElectrodeSelectorProvider;
import io.ast.jneurocarto.core.ProbeProvider;
import io.ast.jneurocarto.probe_npx.NpxProbeProvider;

module io.ast.jneurocarto.probe_npx {
    requires static org.jspecify;
    requires jdk.incubator.vector;

    requires org.slf4j;
    requires info.picocli;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.probe_npx;
    exports io.ast.jneurocarto.probe_npx.blueprint;
    exports io.ast.jneurocarto.probe_npx.select;
    opens io.ast.jneurocarto.probe_npx.cli to info.picocli;

    provides ProbeProvider with NpxProbeProvider;
    provides ElectrodeSelectorProvider with io.ast.jneurocarto.probe_npx.select.ElectrodeSelectorProvider;
}