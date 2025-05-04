module io.ast.jneurocarto {
    requires static org.jspecify;
    requires org.slf4j;
    requires info.picocli;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires flow.server;

    opens io.ast.jneurocarto.core.cli to info.picocli;

}