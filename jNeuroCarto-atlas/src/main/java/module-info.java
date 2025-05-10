module io.ast.jneurocarto.atlas {

    requires java.net.http;
    requires java.desktop;

    requires static org.jspecify;
    requires org.slf4j;
    requires info.picocli;
    requires com.fasterxml.jackson.databind;

    exports io.ast.jneurocarto.atlas;
    opens io.ast.jneurocarto.atlas.cli to info.picocli;
}