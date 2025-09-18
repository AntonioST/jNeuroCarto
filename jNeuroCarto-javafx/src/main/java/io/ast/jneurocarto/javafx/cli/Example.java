package io.ast.jneurocarto.javafx.cli;

import picocli.CommandLine;

public class Example {
    public static void main(String[] args) {
        var parser = new CommandLine(new io.ast.jneurocarto.javafx.chart.cli.Main());
        parser.setCaseInsensitiveEnumValuesAllowed(true);
        parser.addSubcommand(new Complete());
        parser.addSubcommand(new FlashText());
        parser.execute(args);
    }
}
