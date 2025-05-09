package io.ast.jneurocarto.core;

public interface ProbeProvider {

    String name();

    default String description() {
        return "";
    }

    ProbeDescription<?> getProbeDescription();
}
