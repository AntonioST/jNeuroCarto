package io.ast.jneurocarto.probe_npx.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import io.ast.jneurocarto.probe_npx.NpxProbeType;
import picocli.CommandLine;

@CommandLine.Command(
  name = "efficiency",
  sortOptions = false,
  usageHelpAutoWidth = true,
  mixinStandardHelpOptions = true,
  description = "calculate channelmap efficiency"
)
public class Efficiency implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "CHMAP",
      description = "channelmap file.")
    Path chmapFile;

    @CommandLine.Parameters(index = "1", paramLabel = "BLUEPRINT",
      description = "blueprint file.")
    Path blueprintFile;

    @CommandLine.Option(names = "--detail",
      description = "show detailed results")
    boolean detail;

    private Logger log;

    @Override
    public void run() {
        log = LoggerFactory.getLogger(Density.class);
        log.debug("run()");

        try {
            compute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void compute() throws IOException {
        log.debug("chmapFile={}", chmapFile);
        if (!Files.exists(chmapFile)) throw new RuntimeException("channelmap file not existed : " + chmapFile);

        log.debug("blueprintFile={}", blueprintFile);
        if (!Files.exists(blueprintFile)) throw new RuntimeException("blueprint file not existed : " + blueprintFile);

        var desp = new NpxProbeDescription();
        var chmap = loadChannelmapFile(desp, chmapFile);
        var blueprint = loadBlueprintFile(desp, chmap, blueprintFile);
        var efficiency = ChannelMaps.channelEfficiency(blueprint);

        if (!detail) {
            System.out.printf("%.2f\n", efficiency.efficiency() * 100);
        } else {
            if (chmap.type() instanceof NpxProbeType.NP2020) {
                showDetailResultsNP2020(blueprint, efficiency);
            } else {
                showDetailResults(efficiency);
            }
        }
    }

    private ChannelMap loadChannelmapFile(NpxProbeDescription desp, Path chmapFile) throws IOException {
        log.debug("load(chmapFile)");
        var chmap = desp.load(chmapFile);
        log.debug("probe.type={}", chmap.type().name());
        return chmap;
    }

    private Blueprint<ChannelMap> loadBlueprintFile(NpxProbeDescription desp, ChannelMap chmap, Path blueprintFile) throws IOException {
        log.debug("load(blueprintFile)");
        var electrodes = desp.loadBlueprint(blueprintFile, chmap);
        return new Blueprint<>(desp, chmap, electrodes);
    }

    private void showDetailResults(ChannelMaps.Efficiency efficiency) {
        System.out.printf("Used channel  %d\n", efficiency.used());
        System.out.printf("Total channel %d\n", efficiency.total());
        System.out.printf("Eff_area      %f%%\n", efficiency.area() * 100);
        System.out.printf("Eff_channel_c %f%%\n", efficiency.channelComplete() * 100);
        System.out.printf("Eff_channel   %f%%\n", efficiency.efficiency() * 100);
    }

    private void showDetailResultsNP2020(Blueprint<ChannelMap> blueprint, ChannelMaps.Efficiency efficiency) {
        assert NpxProbeType.np2020.nShank() == 4;
        var toolkit = new BlueprintToolkit<>(blueprint);
        var s0 = toolkit.mask(e -> e.s() == 0);
        var s1 = toolkit.mask(e -> e.s() == 1);
        var s2 = toolkit.mask(e -> e.s() == 2);
        var s3 = toolkit.mask(e -> e.s() == 3);
        var r0 = ChannelMaps.channelEfficiency(toolkit, s0);
        var r1 = ChannelMaps.channelEfficiency(toolkit, s1);
        var r2 = ChannelMaps.channelEfficiency(toolkit, s2);
        var r3 = ChannelMaps.channelEfficiency(toolkit, s3);

        System.out.printf("[*] Used channel  %d/%d\n", efficiency.used(), efficiency.total());
        System.out.printf("[*] Eff_area      %f%%\n", efficiency.area() * 100);
        System.out.printf("[*] Eff_channel_c %f%%\n", efficiency.channelComplete() * 100);
        System.out.printf("[*] Eff_channel   %f%%\n", efficiency.efficiency() * 100);
        showDetailResultsNP2020PerShank(0, r0);
        showDetailResultsNP2020PerShank(1, r1);
        showDetailResultsNP2020PerShank(2, r2);
        showDetailResultsNP2020PerShank(3, r3);
    }

    private static void showDetailResultsNP2020PerShank(int shank, ChannelMaps.Efficiency r3) {
        System.out.printf("[%d] Used channel  %d/%d\n", shank, r3.used(), r3.total());
        System.out.printf("[%d] Eff_channel   %f%%\n", shank, r3.efficiency() * 100);
    }
}
