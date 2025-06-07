package io.ast.jneurocarto.atlas.cli;

import java.util.Comparator;
import java.util.List;

import io.ast.jneurocarto.atlas.BrainGlobeDownloader;
import picocli.CommandLine;

@CommandLine.Command(
    name = "list",
    sortOptions = false,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    description = "list local atlas repository"
)
public class ListLocal implements Runnable {

    @CommandLine.Parameters(index = "0..",
        description = "matched name")
    public List<String> matchNameList = List.of();

    @CommandLine.Mixin
    public Main.ConfigOptions config;

    @Override
    public void run() {
        var downloader = BrainGlobeDownloader.builder()
            .setConfig(config.getConfig());

        List<BrainGlobeDownloader.DownloadResult> results;

        if (matchNameList.isEmpty()) {
            results = downloader.findAll();
        } else {
            results = downloader.findAll(name -> matchNameList.stream().anyMatch(name::contains));
        }

        results.stream()
            .sorted(Comparator.comparing(BrainGlobeDownloader.DownloadResult::atlas))
            .map(BrainGlobeDownloader.DownloadResult::atlasNameVersion)
            .forEach(System.out::println);
    }

}
