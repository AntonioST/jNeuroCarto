import io.ast.jneurocarto.javafx.app.DefaultPluginProvider;
import io.ast.jneurocarto.javafx.view.PluginProvider;

module io.ast.jneurocarto.javafx {

    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.github.classgraph;
    requires info.picocli;
    requires org.slf4j;

    requires static org.jspecify;

    requires io.ast.jneurocarto.core;
    requires io.ast.jneurocarto.atlas;
    requires io.ast.jneurocarto.javafx.chart;

    exports io.ast.jneurocarto.javafx.app;
    exports io.ast.jneurocarto.javafx.atlas;
    exports io.ast.jneurocarto.javafx.blueprint;
    exports io.ast.jneurocarto.javafx.cli;
    exports io.ast.jneurocarto.javafx.script;
    exports io.ast.jneurocarto.javafx.utils;
    exports io.ast.jneurocarto.javafx.view;

    uses PluginProvider;
    provides PluginProvider with DefaultPluginProvider;

    opens io.ast.jneurocarto.javafx.cli to info.picocli;
}