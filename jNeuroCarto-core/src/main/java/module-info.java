import io.ast.jneurocarto.core.ElectrodeSelectorProvider;
import io.ast.jneurocarto.core.ProbeProvider;
import io.ast.jneurocarto.core.RandomElectrodeSelectorProvider;

module io.ast.jneurocarto.core {
    requires static org.jspecify;

    exports io.ast.jneurocarto.core;

    uses ElectrodeSelectorProvider;
    uses ProbeProvider;
    provides ElectrodeSelectorProvider with RandomElectrodeSelectorProvider;
}