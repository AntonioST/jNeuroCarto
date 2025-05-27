import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.probe_npx.javafx.NpxProbePluginProvider;

module io.ast.jneurocarto.probe_npx.javafx {

    requires static org.jspecify;

    requires javafx.graphics;
    requires javafx.controls;

    requires org.slf4j;

    requires io.ast.jneurocarto.core;
    requires io.ast.jneurocarto.javafx;
    requires io.ast.jneurocarto.probe_npx;
    requires java.desktop;

    exports io.ast.jneurocarto.probe_npx.javafx;

    provides PluginProvider with NpxProbePluginProvider;
}
