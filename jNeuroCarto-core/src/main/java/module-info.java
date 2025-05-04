import io.ast.jneurocarto.core.ElectrodeSelectorProvider;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.RandomElectrodeSelectorProvider;

module io.ast.jneurocarto.core {
    requires static org.jspecify;
    requires org.slf4j;

    exports io.ast.jneurocarto.core;

    uses ProbeDescription;
    uses ElectrodeSelectorProvider;
    provides ElectrodeSelectorProvider with RandomElectrodeSelectorProvider;
}