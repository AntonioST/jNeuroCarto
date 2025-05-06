package io.ast.jneurocarto.probe_npx.cli;

import org.slf4j.LoggerFactory;

import picocli.CommandLine;

@CommandLine.Command(
  name = "jneurocarto-npx",
  usageHelpWidth = 120,
  usageHelpAutoWidth = true,
  description = "",
  subcommands = {
    Info.class,
    Read.class,
    Select.class,
  }
)
public class Main implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--debug")
    public void debug(boolean value) {
        if (value) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto.probe_npx", "debug");
        }
    }

    private static CommandLine parser;

    public static void main(String[] args) {
        parser = new CommandLine(new Main());
        System.exit(parser.execute(args));
    }

    public void run() {
        var log = LoggerFactory.getLogger(getClass());
        log.debug("run()");
        parser.usage(System.out);
    }
}
