package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class BrainAtlas {

    public static final String METADATA_FILENAME = "metadata.json";
    public static final String STRUCTURES_FILENAME = "structures.json";
    public static final String REFERENCE_FILENAME = "reference.tiff";
    public static final String ANNOTATION_FILENAME = "annotation.tiff";
    public static final String HEMISPHERES_FILENAME = "hemispheres.tiff";
    public static final String MESHES_DIRNAME = "meshes";

    private final BrainAtlasMeta meta;

    public BrainAtlas(BrainAtlasMeta meta) {
        this.meta = meta;
    }

    public static BrainAtlas load(Path dir) throws IOException {
        var meta = BrainAtlasMeta.load(dir.resolve(METADATA_FILENAME));
        return new BrainAtlas(meta);
    }

    public static BrainAtlas load(String name) throws IOException {
        return BrainAtlasDownloader.downloader(name).download().get();
    }

    public static BrainAtlas load(String name, Path downloadDir, boolean checkLatest) throws IOException {
        return BrainAtlasDownloader.downloader(name)
          .setDownloadDir(downloadDir)
          .setCheckLatest(checkLatest)
          .download()
          .get();
    }

}
