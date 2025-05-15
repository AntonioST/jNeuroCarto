package io.ast.jneurocarto.config.cli;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        System.exit(new CommandLine(new CartoConfig()).execute(args));
    }

}
