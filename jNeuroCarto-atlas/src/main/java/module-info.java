module io.ast.jneurocarto.atlas {

    requires java.net.http;

    requires static java.desktop;
    requires static javafx.graphics;

    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.compress;
    requires org.slf4j;

    requires static info.picocli;
    requires static org.jspecify;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.atlas;
    exports io.ast.jneurocarto.atlas.cli;

    opens io.ast.jneurocarto.atlas.cli to info.picocli;
}