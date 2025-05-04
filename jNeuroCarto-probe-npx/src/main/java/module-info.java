import io.ast.jneurocarto.core.ElectrodeSelectorProvider;
import io.ast.jneurocarto.core.ProbeProvider;
import io.ast.jneurocarto.probe_npx.NeuropixelsProbeProvider;

module io.ast.jneurocarto.probe_npx {
    requires static org.jspecify;
    requires org.slf4j;
    requires jdk.incubator.vector;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.probe_npx;

    provides ProbeProvider with NeuropixelsProbeProvider;
    provides ElectrodeSelectorProvider with io.ast.jneurocarto.probe_npx.select.ElectrodeSelectorProvider;
}