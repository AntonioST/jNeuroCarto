module io.ast.jneurocarto.atlas {

    requires java.net.http;

    requires static org.jspecify;
    requires org.slf4j;
    requires info.picocli;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.compress;

    requires static java.desktop;
    requires static javafx.graphics;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.atlas;
    exports io.ast.jneurocarto.atlas.cli;
    opens io.ast.jneurocarto.atlas.cli to info.picocli;
}