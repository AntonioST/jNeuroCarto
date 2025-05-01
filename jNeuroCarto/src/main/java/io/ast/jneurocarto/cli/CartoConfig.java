package io.ast.jneurocarto.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@NullMarked
@Command(name = "jneurocarto",
  usageHelpWidth = 120,
  usageHelpAutoWidth = true,
  description = "")
public final class CartoConfig implements Runnable {

    @Parameters(index = "0", paramLabel = "FILE", arity = "0..1", description = "open channelmap file.")
    public @Nullable Path file;

    @Option(names = {"-C", "--chmap-dir"}, paramLabel = "PATH",
      description = "channel saving directory")
    public @Nullable Path chmapRoot;


    @Option(names = {"-P", "--probe"}, paramLabel = "NAME",
      description = "use probe family. default use \"npx\" (Neuropixels probe family).")
    public String probeFamily = "npx";

    @Option(names = "--selector", paramLabel = "NAME",
      description = "use which electrode selection method")
    public @Nullable String probeSelector;


    @Option(names = "--atlas", paramLabel = "NAME", defaultValue = "25",
      converter = AtlasNameConverter.class,
      description = "atlas mouse brain name")
    public String atlasName;

    @Option(names = "--atlas-root", paramLabel = "PATH",
      description = "atlas mouse brain download path")
    public @Nullable Path atlasRoot;

    @Option(names = "--config-file", paramLabel = "FILE",
      description = "user config file.")
    public @Nullable String configFile;

    @Option(names = "--view", paramLabel = "NAME",
      converter = CommaStringListConverter.class,
      description = "install extra views in right panel")
    public List<String> extraViewList;

    @Option(names = "--server-address", paramLabel = "URL",
      description = "address")
    public @Nullable String serverAddress;

    @Option(names = "--server-port", paramLabel = "PORT",
      description = "port")
    public int serverPort = -1;

    @Option(names = "--open-browser",
      negatable = true,
      description = "do not open browser when server starts")
    public boolean noOpenBrowser = true;

    @Option(names = "--debug")
    public boolean debug;

    @Option(names = {"-h", "--help"}, help = true)
    public boolean help;

//    public String openFile;

    public static CommandLine newParser(CartoConfig config) {
        var parser = new CommandLine(config);
        return parser;
    }

    public static void usage(CartoConfig config, PrintStream out) {
        new CommandLine(config).usage(out);
    }

    @Override
    public void run() {
    }

    @Override
    public String toString() {
        return "CartoConfig{" +
               "file=" + file +
               ", chmapRoot=" + chmapRoot +
               ", probeFamily='" + probeFamily + '\'' +
               ", probeSelector='" + probeSelector + '\'' +
               ", atlasName='" + atlasName + '\'' +
               ", atlasRoot=" + atlasRoot +
               ", configFile='" + configFile + '\'' +
               ", extraViewList=" + extraViewList +
               ", serverAddress='" + serverAddress + '\'' +
               ", serverPort=" + serverPort +
               ", noOpenBrowser=" + noOpenBrowser +
               ", debug=" + debug +
               ", help=" + help +
               '}';
    }
}
