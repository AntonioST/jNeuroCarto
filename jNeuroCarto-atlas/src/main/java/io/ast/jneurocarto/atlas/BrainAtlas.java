package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class BrainAtlas {

    public static final String METADATA_FILENAME = "metadata.json";
    public static final String STRUCTURES_FILENAME = "structures.json";
    public static final String REFERENCE_FILENAME = "reference.tiff";
    public static final String ANNOTATION_FILENAME = "annotation.tiff";
    public static final String HEMISPHERES_FILENAME = "hemispheres.tiff";
    public static final String MESHES_DIRNAME = "meshes";

    private final Path root;
    private final BrainAtlasMeta meta;
    private final Structures structures;
    private final AnatomicalSpace space;

    public BrainAtlas(Path root) throws IOException {
        this.root = root;
        meta = BrainAtlasMeta.load(root.resolve(METADATA_FILENAME));
        structures = Structures.load(root.resolve(STRUCTURES_FILENAME));
        space = new AnatomicalSpace(meta.orientation, meta.shape, meta.resolution, null);
    }

    public static BrainAtlas load(String name) throws IOException {
        return BrainGlobeDownloader.builder().atlasName(name).download().get();
    }

    public static BrainAtlas load(String name, Path downloadDir, boolean checkLatest) throws IOException {
        return BrainGlobeDownloader.builder()
          .atlasName(name)
          .setDownloadDir(downloadDir)
          .setCheckLatest(checkLatest)
          .download()
          .get();
    }

    public static Map<String, String> listAtlasNames() {
        return new HashMap<>(BrainGlobeDownloader.builder().getLastVersions());
    }

    /*==================
     * meta information *
     *==================*/

    public double[] resolution() {
        return meta.resolution;
    }

    public String orientation() {
        return meta.orientation;
    }

    public int[] shape() {
        return meta.shape;
    }

    public double[] shapeUm() {
        double[] ret = new double[3];
        for (int i = 0; i < 3; i++) {
            ret[i] = meta.shape[i] * meta.resolution[i];
        }
        return ret;
    }

    /*============
     * image data *
     *============*/

    public ImageVolume reference() throws IOException {
        return ImageVolume.readTiff(root.resolve(REFERENCE_FILENAME));
    }

    public ImageVolume annotation() throws IOException {
        return ImageVolume.readTiff(root.resolve(ANNOTATION_FILENAME));
    }

}
