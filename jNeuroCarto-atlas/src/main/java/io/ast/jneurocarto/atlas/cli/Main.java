package io.ast.jneurocarto.atlas.cli;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.BrainGlobeConfig;
import picocli.CommandLine;

@CommandLine.Command(
  name = "jneurocarto-atlas",
  usageHelpWidth = 120,
  usageHelpAutoWidth = true,
  description = "",
  subcommands = {
    Read.class,
    Remote.class,
    Download.class,
  }
)
public final class Main implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--debug")
    public void debug(boolean value) {
        if (value) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto.atlas", "debug");
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

    public static class ConfigOptions {

        @CommandLine.Spec(CommandLine.Spec.Target.MIXEE)
        CommandLine.Model.CommandSpec mixee;

        @CommandLine.Option(names = "--config", paramLabel = "FILE",
          description = "use which config file")
        public Path configFile;

        public BrainGlobeConfig getConfig() {
            try {
                if (configFile == null) {
                    return BrainGlobeConfig.load();
                } else {
                    return BrainGlobeConfig.load(configFile);
                }
            } catch (IOException e) {
                LoggerFactory.getLogger(mixee.userObject().getClass()).warn("getConfig", e);
                return BrainGlobeConfig.getDefault();
            }
        }
    }
}
