import io.ast.jneurocarto.ProbeDescription;

module io.ast.jneurocarto {
    requires static org.jspecify;
    requires info.picocli;
    requires org.slf4j;

    opens io.ast.jneurocarto.cli to info.picocli;

    uses ProbeDescription;
}