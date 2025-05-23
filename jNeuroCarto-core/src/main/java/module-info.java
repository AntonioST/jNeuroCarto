import io.ast.jneurocarto.core.ElectrodeSelectorProvider;
import io.ast.jneurocarto.core.ProbeProvider;
import io.ast.jneurocarto.core.RandomElectrodeSelectorProvider;

module io.ast.jneurocarto.core {
    requires static org.jspecify;
    requires static info.picocli;

    requires static com.fasterxml.jackson.annotation;
    requires static com.fasterxml.jackson.databind;

    requires org.slf4j;

    exports io.ast.jneurocarto.core;
    exports io.ast.jneurocarto.core.blueprint;
    exports io.ast.jneurocarto.core.numpy;
    exports io.ast.jneurocarto.core.config;
    exports io.ast.jneurocarto.core.cli;
    opens io.ast.jneurocarto.core.cli to info.picocli;

    uses ElectrodeSelectorProvider;
    uses ProbeProvider;
    provides ElectrodeSelectorProvider with RandomElectrodeSelectorProvider;
}