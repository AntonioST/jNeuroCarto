import io.ast.jneurocarto.core.ProbeProvider;

module io.ast.jneurocarto.core {

    requires static javafx.graphics;

    requires org.slf4j;

    requires static com.fasterxml.jackson.annotation;
    requires static com.fasterxml.jackson.databind;
    requires static info.picocli;
    requires static io.github.classgraph;
    requires static org.apache.commons.csv;
    requires static org.jspecify;

    exports io.ast.jneurocarto.core;
    exports io.ast.jneurocarto.core.blueprint;
    exports io.ast.jneurocarto.core.numpy;
    exports io.ast.jneurocarto.core.config;

    exports io.ast.jneurocarto.core.cli;

    uses ProbeProvider;

    opens io.ast.jneurocarto.core.cli to info.picocli;
}