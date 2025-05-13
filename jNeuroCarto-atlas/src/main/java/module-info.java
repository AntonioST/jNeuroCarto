module io.ast.jneurocarto.atlas {

    requires java.net.http;
    requires java.desktop;
    requires javafx.graphics;
    requires javafx.controls;

    requires static org.jspecify;
    requires org.slf4j;
    requires info.picocli;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.compress;

    requires io.ast.jneurocarto.core;

    exports io.ast.jneurocarto.atlas;
    opens io.ast.jneurocarto.atlas.cli to info.picocli;
    exports io.ast.jneurocarto.atlas.gui to javafx.graphics;
}