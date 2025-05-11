package io.ast.jneurocarto.atlas.cli;

import java.util.List;
import java.util.Map;

import io.ast.jneurocarto.atlas.BrainGlobeDownloader;
import picocli.CommandLine;

@CommandLine.Command(
  name = "remote",
  usageHelpAutoWidth = true,
  description = "remote atlas server",
  subcommands = {
    Remote.VersionInfo.class
  }
)
public class Remote implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Mixin
    public Main.ConfigOptions config;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    @CommandLine.Command(name = "version", description = "print last_versions.conf.")
    public static class VersionInfo implements Runnable {

        @CommandLine.ParentCommand
        Remote remote;

        @CommandLine.Option(names = "--force",
          description = "force download from the server.")
        public boolean forceDownload;

        @CommandLine.Parameters(index = "0..",
          description = "matched name")
        public List<String> matchNameList = List.of();

        @Override
        public void run() {
            var downloader = BrainGlobeDownloader.builder()
              .setConfig(remote.config.getConfig());

            var prop = downloader.getLastVersions(forceDownload);
            if (prop == null) {
                throw new RuntimeException("cannot load " + BrainGlobeDownloader.LAST_VERSION_FILENAME);
            } else {
                if (matchNameList.isEmpty()) {
                    prop.keySet().stream()
                      .sorted()
                      .forEach(atlas -> printAtlasNameAndVersion(prop, atlas));
                } else {
                    for (var name : matchNameList) {
                        prop.keySet().stream()
                          .filter(it -> it.contains(name))
                          .sorted()
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
