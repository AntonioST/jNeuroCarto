package io.ast.jneurocarto.probe_npx.cli;

import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.cli.CartoVersionProvider;
import picocli.CommandLine;

@CommandLine.Command(
    name = "jneurocarto-npx",
    sortOptions = false,
    usageHelpWidth = 120,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    versionProvider = CartoVersionProvider.class,
    description = "Neuropixels probe utilities",
    subcommands = {
        Info.class,
        Read.class,
        Create.class,
        Select.class,
        Density.class,
    }
)
public final class Main implements Runnable {

    @CommandLine.Option(names = "--debug")
    public void debug(boolean value) {
        if (value) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto", "debug");
        }
    }

    private static CommandLine parser;

    public static void main(String[] args) {
        parser = new CommandLine(new Main());
        System.exit(parser.execute(args));
    }

    @Override
    public void run() {
        var log = LoggerFactory.getLogger(getClass());
        log.debug("run()");
        parser.usage(System.out);
    }
}
