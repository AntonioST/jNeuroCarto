package io.ast.jneurocarto.atlas.cli;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.BrainGlobeDownloader;
import picocli.CommandLine;

@CommandLine.Command(
  name = "download",
  usageHelpAutoWidth = true,
  description = "download atlas from server"
)
public class Download implements Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Mixin
    public Main.ConfigOptions config;

    @CommandLine.Option(names = "--check", negatable = true,
      description = "check latest version.")
    public boolean checkLatest;

    @CommandLine.Option(names = "--dry",
      description = "dry run and just print. no download actions.")
    public boolean dryrun;

    @CommandLine.Parameters(index = "0..",
      description = "atlas name")
    public List<String> atlasNameList = List.of();

    private Logger log;

    @Override
    public void run() {
        log = LoggerFactory.getLogger(Download.class);

        if (atlasNameList.isEmpty()) {
            listAtlasName();
        } else {
            var downloader = BrainGlobeDownloader.builder()
              .dryrun(dryrun)
              .setConfig(config.getConfig())
              .setCheckLatest(checkLatest);

            if (downloader.dryrun()) log.info("dryrun mode");

            for (var atlas : atlasNameList) {
                try {
                    download(downloader, atlas);
                } catch (IOException e) {
                    log.warn("download " + atlas, e);
                }
            }
        }
    }

    private void listAtlasName() {
        var downloader = BrainGlobeDownloader.builder()
          .dryrun(dryrun)
          .setConfig(config.getConfig());

        var prop = downloader.getLastVersions(false);
        if (prop == null) {
            throw new RuntimeException("cannot load " + BrainGlobeDownloader.LAST_VERSION_FILENAME);
        } else {
            var downloadDir = downloader.getDownloadDir();

            prop.keySet().stream()
              .map(it -> (String) it)
              .sorted()
              .forEach(atlas -> {
                  var version = prop.getProperty(atlas);
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

    public void download(BrainGlobeDownloader downloader, String atlas) throws IOException {
        downloader.atlasName(atlas).download();
    }
}
