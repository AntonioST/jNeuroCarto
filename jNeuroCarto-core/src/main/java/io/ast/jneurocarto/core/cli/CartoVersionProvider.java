package io.ast.jneurocarto.core.cli;

import picocli.CommandLine;

public class CartoVersionProvider implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        return new String[]{System.getProperty("jpackage.app-version", "develop")};
    }
}
