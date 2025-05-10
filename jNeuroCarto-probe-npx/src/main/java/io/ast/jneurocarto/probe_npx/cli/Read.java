package io.ast.jneurocarto.probe_npx.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import picocli.CommandLine;

@CommandLine.Command(
  name = "read",
  usageHelpAutoWidth = true,
  description = "read file"
)
public final class Read implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = "file path")
    Path file;

    enum FileType {
        channelmap, blueprint
    }

    @CommandLine.Option(names = {"-t", "--type"}, paramLabel = "TYPE",
      description = "force consider file as given file-type.")
    FileType filetype;

    private Logger log;

    @Override
    public void run() {
        log = LoggerFactory.getLogger(getClass());
        log.debug("run(file={})", file);

        if (!Files.exists(file)) {
            throw new RuntimeException("file not exist : " + file);
        }

        if (filetype == null) {
            var filename = file.getFileName().toString();
            var last = filename.lastIndexOf('.');
            var suffix = filename.substring(last);
            filetype = switch (suffix) {
                case ".npy" -> FileType.blueprint;
                case ".imro", ".meta" -> FileType.channelmap;
                default -> throw new RuntimeException("unknown file suffix : " + suffix);
            };

            log.debug("use(filetype={})", filetype.name());
        }

        try {
            switch (filetype) {
            case channelmap -> readChannelmap(file);
            case blueprint -> readBlueprint(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readChannelmap(Path file) throws IOException {
        var desp = new NpxProbeDescription();

        log.debug("load.channelmap(file={})", file);
        var chmap = desp.load(file);
        printChannelmap(file, chmap);
    }

    public void readBlueprint(Path file) throws IOException {
        var desp = new NpxProbeDescription();

        log.debug("load.blueprint(file={})", file);
        var electrodes = desp.loadBlueprint(file);

        printBlueprint(file, electrodes);
    }

    public void printChannelmap(Path file, ChannelMap chmap) {
        System.out.println("Channelmap:");
        System.out.printf("%-16s %s\n", "code", chmap.type().name());
        System.out.printf("%-16s %d\n", "nChannels", chmap.size());
        System.out.printf("%-16s %d\n", "totalChannels", chmap.nChannel());
        System.out.printf("%-16s %s\n", "reference", chmap.getReferenceInfo());

        var meta = chmap.getMeta();
        if (meta != null) {
            System.out.printf("%-16s %s\n", "serialNumber", meta.serialNumber());
        }

        ChannelMaps.printProbe(System.out, chmap, true);
    }

    public void printBlueprint(Path file, List<ElectrodeDescription> electrodes) {
        System.out.println("Blueprint:");
        System.out.printf("%-16s %d\n", "nElectrode", electrodes.size());
        System.out.printf("%-16s %d\n", "nE(FULL)", countElectrodeInCategory(electrodes, NpxProbeDescription.CATE_FULL));
        System.out.printf("%-16s %d\n", "nE(HALF)", countElectrodeInCategory(electrodes, NpxProbeDescription.CATE_HALF));
        System.out.printf("%-16s %d\n", "nE(QUARTER)", countElectrodeInCategory(electrodes, NpxProbeDescription.CATE_QUARTER));
        System.out.printf("%-16s %d\n", "nE(LOW)", countElectrodeInCategory(electrodes, NpxProbeDescription.CATE_LOW));
        System.out.printf("%-16s %d\n", "nE(UNSET)", countElectrodeInCategory(electrodes, NpxProbeDescription.CATE_UNSET));
        System.out.printf("%-16s %d\n", "nE(EXCLUDE)", countElectrodeInCategory(electrodes, NpxProbeDescription.CATE_EXCLUDED));
    }

    private static long countElectrodeInCategory(List<ElectrodeDescription> electrode, int cateFull) {
        return electrode.stream().filter(e -> e.category() == cateFull).count();
    }
}
