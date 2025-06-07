package io.ast.jneurocarto.atlas.cli;

import java.util.List;
import java.util.Map;

import io.ast.jneurocarto.atlas.BrainGlobeDownloader;
import picocli.CommandLine;

@CommandLine.Command(
    name = "remote",
    sortOptions = false,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    description = "remote atlas server",
    subcommands = {
        Remote.VersionInfo.class
    }
)
public class Remote implements Runnable {

    @CommandLine.Mixin
    public Main.ConfigOptions config;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    @CommandLine.Command(
        name = "version",
        description = "print last_versions.conf.",
        sortOptions = false
    )
    public static class VersionInfo implements Runnable {

        @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
        public boolean help;

        @CommandLine.Option(names = "--force",
            description = "force download from the server.")
        public boolean forceDownload;

        @CommandLine.Option(names = "--simple",
            description = "just print version number, which require exact-match one name.")
        public boolean simple;

        @CommandLine.Parameters(index = "0..",
            description = "matched name")
        public List<String> matchNameList = List.of();

        @CommandLine.ParentCommand
        Remote remote;

        @Override
        public void run() {
            var downloader = BrainGlobeDownloader.builder()
                .setConfig(remote.config.getConfig());

            var prop = downloader.getLastVersions(forceDownload);
            if (prop == null) {
                throw new RuntimeException("cannot load " + BrainGlobeDownloader.LAST_VERSION_FILENAME);
            } else if (simple) {
                if (matchNameList.isEmpty()) {
                    throw new RuntimeException("require exact-match name");
                } else {
                    for (var name : matchNameList) {
                        var version = prop.get(name);
                        System.out.println(version == null ? "" : version);
                    }
                }
            } else {
                if (matchNameList.isEmpty()) {
                    prop.keySet().stream()
                        .sorted()
                        .forEach(atlas -> printAtlasNameAndVersion(prop, atlas));
                } else {
                    for (var name : matchNameList) {
                        prop.keySet().stream()
                            .filter(it -> it.contains(name))
                            .sorted(String::compareToIgnoreCase)
                            .forEach(atlas -> printAtlasNameAndVersion(prop, atlas));
                    }
                }
            }
        }

        private static void printAtlasNameAndVersion(Map<String, String> prop, String atlas) {
            System.out.printf("%-16s = %s\n", atlas, prop.get(atlas));
        }
    }
}
