import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.probe_npx.javafx.NpxProbePluginProvider;

module io.ast.jneurocarto.probe_npx.javafx {

    requires java.desktop;
    requires javafx.controls;
    requires javafx.graphics;

    requires com.fasterxml.jackson.annotation;
    requires org.slf4j;

    requires static org.jspecify;

    requires io.ast.jneurocarto.core;
    requires io.ast.jneurocarto.javafx;
    requires io.ast.jneurocarto.javafx.chart;
    requires io.ast.jneurocarto.probe_npx;

    exports io.ast.jneurocarto.probe_npx.javafx;

    provides PluginProvider with NpxProbePluginProvider;
}
