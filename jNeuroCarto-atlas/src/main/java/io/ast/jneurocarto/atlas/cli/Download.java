package io.ast.jneurocarto.atlas.cli;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;

@CommandLine.Command(
    name = "download",
    sortOptions = false,
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

    @CommandLine.Option(names = "--force",
        description = "force downloading latest version.")
    public boolean force;

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

        var use = new Main.UseAtlas();
        var downloader = use.newDownloader(config)
            .setCheckLatest(checkLatest)
            .dryrun(dryrun);

        if (atlasNameList.isEmpty()) {
            use.listAtlasName(downloader);
        } else {
            if (downloader.dryrun()) log.info("dryrun mode");

            for (var atlas : atlasNameList) {
                try {
                    use.download(downloader, atlas, force);
                } catch (IOException e) {
                    log.warn("download " + atlas, e);
                }
            }
        }
    }
}
