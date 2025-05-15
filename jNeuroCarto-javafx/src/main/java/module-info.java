module io.ast.jneurocarto.javafx {
    requires static org.jspecify;

    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;

    requires org.slf4j;
    requires info.picocli;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    requires io.ast.jneurocarto.core;
    requires io.ast.jneurocarto.config;
    requires io.ast.jneurocarto.probe_npx;
    requires io.ast.jneurocarto.atlas;

    exports io.ast.jneurocarto.javafx.app;
    opens io.ast.jneurocarto.javafx.app to io.ast.jneurocarto.javafx.base;

    exports io.ast.jneurocarto.javafx.view;
    uses io.ast.jneurocarto.javafx.view.PluginProvider;
    uses io.ast.jneurocarto.javafx.view.ProbePluginProvider;

    opens io.ast.jneurocarto.javafx.cli to info.picocli;
    exports io.ast.jneurocarto.javafx.cli to javafx.graphics;

}