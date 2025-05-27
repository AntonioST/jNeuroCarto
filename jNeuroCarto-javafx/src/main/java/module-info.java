import io.ast.jneurocarto.javafx.app.DefaultPluginProvider;
import io.ast.jneurocarto.javafx.view.PluginProvider;

module io.ast.jneurocarto.javafx {
    requires static org.jspecify;

    requires javafx.base;
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.controls;

    requires org.slf4j;
    requires info.picocli;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.github.classgraph;

    requires io.ast.jneurocarto.core;
    requires io.ast.jneurocarto.atlas;

    exports io.ast.jneurocarto.javafx.app;
    exports io.ast.jneurocarto.javafx.app.dialog;
    exports io.ast.jneurocarto.javafx.atlas;
    exports io.ast.jneurocarto.javafx.blueprint;
    exports io.ast.jneurocarto.javafx.chart;
    exports io.ast.jneurocarto.javafx.script;
    exports io.ast.jneurocarto.javafx.utils;
    exports io.ast.jneurocarto.javafx.view;

    uses PluginProvider;
    provides PluginProvider with DefaultPluginProvider;

    opens io.ast.jneurocarto.javafx.cli to info.picocli;
    exports io.ast.jneurocarto.javafx.cli to javafx.graphics;
}