package io.ast.jneurocarto.core.cli;

import java.nio.file.Path;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "jneurocarto",
    sortOptions = false,
    usageHelpWidth = 120,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    versionProvider = CartoVersionProvider.class,
    description = "jneurocarto application"
)
public final class CartoConfig implements Runnable {

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
        description = "atlas mouse brain name. Use empty string to disable atlas plugin.")
    public String atlasName;

    @ArgGroup(/*exclusive = true, multiplicity = "0..1"*/)
    public AtlasConfig atlasConfig;

    public static class AtlasConfig {
        @Option(names = "--atlas-root", paramLabel = "PATH",
            description = "atlas mouse brain download path.")
        public Path atlasRoot;

        @Option(names = "--atlas-config", paramLabel = "PATH",
            description = "atlas brain config path")
        public Path atlasConfig;
    }

    @Option(names = "--config-file", paramLabel = "FILE",
        description = "user config file.")
    public String configFile;

    @Option(names = "--view", paramLabel = "NAME",
        converter = CommaStringListConverter.class,
        description = "install extra views in right panel")
    public List<String> extraViewList = List.of();

    @Option(names = "--debug",
        description = "enable debug logging message. It reads system property \"io.ast.jneurocarto.debug\".")
    public boolean debug = !System.getProperty("io.ast.jneurocarto.debug", "").isEmpty();

    public static void main(String[] args) {
        System.err.println("It is an test main.");
        System.exit(new CommandLine(new CartoConfig()).execute(args));
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
               ", atlasRoot=" + atlasConfig.atlasRoot +
               ", atlasConfig=" + atlasConfig.atlasConfig +
               ", configFile='" + configFile + '\'' +
               ", extraViewList=" + extraViewList +
               ", debug=" + debug +
               '}';
    }
}
