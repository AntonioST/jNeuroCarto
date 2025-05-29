import io.ast.jneurocarto.javafx.view.PluginProvider;
import io.ast.jneurocarto.script.probe_npx.Provider;

module io.ast.jneurocarto.kotlin {
    requires static org.jspecify;

    requires org.slf4j;
    requires kotlin.stdlib;

    requires io.ast.jneurocarto.core;
    requires io.ast.jneurocarto.probe_npx;
    requires io.ast.jneurocarto.javafx;
    requires io.ast.jneurocarto.probe_npx.javafx;

    exports io.ast.jneurocarto.script;
    exports io.ast.jneurocarto.script.probe_npx;

    provides PluginProvider with Provider;
}