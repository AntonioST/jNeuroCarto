package io.ast.jneurocarto.atlas.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.BrainAtlasMeta;
import io.ast.jneurocarto.atlas.Structures;
import picocli.CommandLine;

@CommandLine.Command(
  name = "read",
  usageHelpAutoWidth = true,
  description = "read file"
)
public class Read implements Runnable {
    @CommandLine.Parameters(index = "0", paramLabel = "FILE", description = "file path")
    Path file;

    enum FileType {
        meta, structure
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
            filetype = switch (filename) {
                case BrainAtlas.METADATA_FILENAME -> FileType.meta;
                case BrainAtlas.STRUCTURES_FILENAME -> FileType.structure;
                default -> throw new RuntimeException("unknown file type : " + filename);
            };

            log.debug("use(filetype={})", filetype.name());
        }

        try {
            switch (filetype) {
            case meta -> readMeta(file);
            case structure -> readStructure(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void readMeta(Path file) throws IOException {
        var meta = BrainAtlasMeta.load(file);
        System.out.println(meta);
    }

    private void readStructure(Path file) throws IOException {
        var structure = Structures.load(file);
        for (var node : structure) {
            System.out.println(node);
        }
    }


}
