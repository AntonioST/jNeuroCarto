import io.ast.jneurocarto.core.ProbeDescription;

module io.ast.jneurocarto.core {
    requires static org.jspecify;
    requires org.slf4j;

    exports io.ast.jneurocarto.core;

    uses ProbeDescription;
}