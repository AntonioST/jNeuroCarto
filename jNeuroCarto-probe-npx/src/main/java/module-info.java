module io.ast.jneurocarto.probe_npx {
    requires static org.jspecify;
    requires org.slf4j;
    requires jdk.incubator.vector;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.probe_npx;

    provides io.ast.jneurocarto.core.ProbeDescription with io.ast.jneurocarto.probe_npx.NeuropixelsProbeDescription;
}