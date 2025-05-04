package io.ast.jneurocarto.core;

public interface ProbeProvider {

    String provideProbeFamily();

    ProbeDescription<?> getProbeDescription();
}
