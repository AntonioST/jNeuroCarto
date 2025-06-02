module io.ast.jneurocarto.javafx.chart {

    requires java.desktop;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;

    requires org.slf4j;

    requires static info.picocli;
    requires static org.jspecify;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.javafx.chart;
    exports io.ast.jneurocarto.javafx.chart.cli;
    exports io.ast.jneurocarto.javafx.chart.colormap;
    exports io.ast.jneurocarto.javafx.chart.data;
    exports io.ast.jneurocarto.javafx.chart.event;
    exports io.ast.jneurocarto.javafx.chart.utils;

    opens io.ast.jneurocarto.javafx.chart.cli to info.picocli;
}