import io.ast.jneurocarto.ProbeDescription;

module io.ast.jneurocarto {
    requires static org.jspecify;
    requires org.slf4j;
    requires info.picocli;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires flow.server;
    requires jdk.incubator.vector;

    opens io.ast.jneurocarto.cli to info.picocli;

    uses ProbeDescription;
}