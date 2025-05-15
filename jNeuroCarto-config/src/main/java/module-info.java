module io.ast.jneurocarto.config {
    requires static org.jspecify;

    requires org.slf4j;

    requires info.picocli;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.config;
    exports io.ast.jneurocarto.config.cli;
    opens io.ast.jneurocarto.config.cli to info.picocli;
}