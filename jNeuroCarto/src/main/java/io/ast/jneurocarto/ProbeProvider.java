package io.ast.jneurocarto;

public interface ProbeProvider {

    String provideProbeFamily();

    ProbeDescription<?> getProbeDescription();
}
