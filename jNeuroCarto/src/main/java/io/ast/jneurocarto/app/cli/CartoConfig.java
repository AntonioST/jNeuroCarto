package io.ast.jneurocarto.app.cli;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Component
@Command(name = "jneurocarto",
  usageHelpWidth = 120,
  usageHelpAutoWidth = true,
  description = "")
public final class CartoConfig implements Runnable {

    @Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @Parameters(index = "0", paramLabel = "FILE", arity = "0..1", description = "open channelmap file.")
    public Path file;

    @Option(names = {"-C", "--chmap-dir"}, paramLabel = "PATH",
      description = "channel saving directory")
    public Path chmapRoot;


    @Option(names = {"-P", "--probe"}, paramLabel = "NAME", defaultValue = "npx",
      description = "use probe family. default use \"npx\" (Neuropixels probe family).")
    public String probeFamily;

    @Option(names = "--selector", paramLabel = "NAME",
      description = "use which electrode selection method")
    public String probeSelector;


    @Option(names = "--atlas", paramLabel = "NAME", defaultValue = "25",
      converter = AtlasNameConverter.class,
      description = "atlas mouse brain name")
    public String atlasName;

    @Option(names = "--atlas-root", paramLabel = "PATH",
      description = "atlas mouse brain download path")
    public Path atlasRoot;

    @Option(names = "--config-file", paramLabel = "FILE",
      description = "user config file.")
    public String configFile;

    @Option(names = "--view", paramLabel = "NAME",
      converter = CommaStringListConverter.class,
      description = "install extra views in right panel")
    public List<String> extraViewList = List.of();

    @Option(names = "--server-address", paramLabel = "URL",
      description = "address")
    public String serverAddress;

    @Option(names = "--server-port", paramLabel = "PORT",
      description = "port")
    public int serverPort = -1;

    @Option(names = "--open-browser",
      negatable = true,
      description = "do not open browser when server starts")
    public boolean noOpenBrowser = true;

    @CommandLine.Option(names = "--debug")
    public void debug(boolean value) {
        if (value) {
            System.setProperty("org.slf4j.simpleLogger.log.io.ast.jneurocarto", "debug");
        }
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
               ", help=" + help +
               '}';
    }
}
