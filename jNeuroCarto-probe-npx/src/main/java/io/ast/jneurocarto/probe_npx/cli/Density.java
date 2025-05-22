package io.ast.jneurocarto.probe_npx.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.numpy.Numpy;
import io.ast.jneurocarto.probe_npx.ChannelMaps;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import picocli.CommandLine;

@CommandLine.Command(
  name = "density",
  usageHelpAutoWidth = true,
  description = "print density array"
)
public final class Density implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "CHMAP",
      description = "channelmap file.")
    Path chmapFile;

    @CommandLine.Option(names = "--dy", paramLabel = "VALUE", defaultValue = "5")
    double dy;

    @CommandLine.Option(names = {"-s", "--smooth"}, paramLabel = "VALUE", defaultValue = "25")
    double smooth;

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "FILE", required = true,
      description = "output to a numpy file")
    Path outputFile;

    private Logger log;

    @Override
    public void run() {
        log = LoggerFactory.getLogger(Density.class);
        log.debug("run()");

        if (dy <= 0) throw new IllegalArgumentException("--dy = " + dy);
        if (smooth < 0) throw new IllegalArgumentException("--smooth = " + smooth);

        try {
            compute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void compute() throws IOException {
        log.debug("chmapFile={}", chmapFile);
        if (!Files.exists(chmapFile)) throw new RuntimeException("channelmap file not existed : " + chmapFile);

        var desp = new NpxProbeDescription();

        log.debug("load(chmapFile)");
        var chmap = desp.load(chmapFile);
        log.debug("probe.type={}", chmap.type().name());

        var density = ChannelMaps.calculateElectrodeDensity(chmap, dy, smooth);

        log.debug("outputFile={}", outputFile);
        Files.createDirectories(outputFile.toAbsolutePath().getParent());
        Numpy.write(outputFile, density);
    }
}
