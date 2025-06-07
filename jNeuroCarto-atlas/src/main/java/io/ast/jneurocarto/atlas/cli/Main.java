package io.ast.jneurocarto.atlas.cli;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.BrainGlobeConfig;
import io.ast.jneurocarto.atlas.BrainGlobeDownloader;
import io.ast.jneurocarto.core.cli.CartoVersionProvider;
import picocli.CommandLine;

@CommandLine.Command(
    name = "jneurocarto-atlas",
    sortOptions = false,
    usageHelpWidth = 120,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    versionProvider = CartoVersionProvider.class,
    description = "Atlas utilities",
    subcommands = {
        Read.class,
        Remote.class,
        ListLocal.class,
        Download.class,
        Use.class,
    }
)
public final class Main implements Runnable {

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

    public static class UseAtlas {

        @CommandLine.Option(names = {"-n", "--name"}, paramLabel = "NAME", required = true,
            description = "use atlas name")
        public String name;

        @CommandLine.Option(names = "--check", negatable = true,
            description = "check latest version.")
        public boolean checkLatest;

        public BrainGlobeDownloader newDownloader(BrainGlobeConfig config) {
            return BrainGlobeDownloader.builder()
                .setConfig(config)
                .setCheckLatest(checkLatest);
        }

        public BrainGlobeDownloader newDownloader(ConfigOptions config) {
            return BrainGlobeDownloader.builder()
                .setConfig(config.getConfig())
                .setCheckLatest(checkLatest);
        }

        public void listAtlasName(BrainGlobeDownloader downloader) {
            var prop = downloader.getLastVersions(false);
            if (prop == null) {
                throw new RuntimeException("cannot load " + BrainGlobeDownloader.LAST_VERSION_FILENAME);
            } else {
                var downloadDir = downloader.getDownloadDir();

                prop.keySet().stream()
                    .sorted()
                    .forEach(atlas -> {
                        var version = prop.get(atlas);
                        System.out.printf("%-16s = %s %s\n", atlas, version, checkStatus(downloadDir, atlas, version));
                    });
            }
        }

        private static String checkStatus(Path downloadDir, String atlas, String version) {
            if (Files.exists(downloadDir.resolve(atlas + "_v" + version))) {
                return "(installed)";
            }

            var pattern = FileSystems.getDefault().getPathMatcher("glob:" + atlas + "_v*");
            try (var dirs = Files.list(downloadDir)) {
                if (dirs.anyMatch(pattern::matches)) {
                    return "(out-of-date)";
                }
            } catch (IOException e) {
            }

            return "";
        }

        public BrainAtlas download(BrainGlobeDownloader downloader) throws IOException {
            return download(downloader, name, false);
        }

        public BrainAtlas download(BrainGlobeDownloader downloader, String name, boolean force) throws IOException {
            return downloader.atlasName(name).download(force).get();
        }
    }
}
