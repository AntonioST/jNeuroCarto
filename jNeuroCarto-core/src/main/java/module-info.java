import io.ast.jneurocarto.core.ProbeProvider;

module io.ast.jneurocarto.core {
    requires static org.jspecify;
    requires static info.picocli;

    requires static com.fasterxml.jackson.annotation;
    requires static com.fasterxml.jackson.databind;

    requires static org.apache.commons.csv;

    requires org.slf4j;
    requires java.xml;
    requires io.github.classgraph;

    exports io.ast.jneurocarto.core;
    exports io.ast.jneurocarto.core.blueprint;
    exports io.ast.jneurocarto.core.numpy;
    exports io.ast.jneurocarto.core.config;
    exports io.ast.jneurocarto.core.cli;
    opens io.ast.jneurocarto.core.cli to info.picocli;


    uses ProbeProvider;

}