package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
        var ret = BrainGlobeDownloader.builder().getLastVersions();
        return ret == null ? Map.of() : new HashMap<>(ret);
    }

    /*==================*
     * meta information *
     *==================*/

    public Path root() {
        return root;
    }

    public String version() {
        return meta.version;
    }

    public BrainAtlasMeta meta() {
        return meta;
    }

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

    public Structures hierarchy() {
        return structures;
    }

    /*============*
     * image data *
     *============*/

    private @Nullable ImageVolume reference;
    private @Nullable ImageVolume annotation;
    private @Nullable ImageVolume hemispheres;

    public ImageVolume reference() throws IOException {
        if (reference == null) {
            reference = ImageVolume.readTiff(root.resolve(REFERENCE_FILENAME));
        }
        return reference;
    }

    public ImageVolume annotation() throws IOException {
        if (annotation == null) {
            annotation = ImageVolume.readTiff(root.resolve(ANNOTATION_FILENAME));
        }
        return annotation;
    }

    public ImageVolume hemispheres() throws IOException {
        if (hemispheres == null) {
            // If reference is symmetric generate hemispheres block
            if (meta.symmetric) {
                // TODO Are they different really?
                hemispheres = ImageVolume.readTiff(root.resolve(HEMISPHERES_FILENAME));
            } else {
                hemispheres = ImageVolume.readTiff(root.resolve(HEMISPHERES_FILENAME));
            }
        }
        return hemispheres;
    }

    /*============*
     * coordinate *
     *============*/

    // TODO how to cooperate with AnatomicalSpace?

    /**
     * coordinate system in anatomical space.
     *
     * @param ap um
     * @param dv um
     * @param ml um
     */
    public record Coordinate(double ap, double dv, double ml) {
        public CoordinateIndex toCoorIndex(double resolution) {
            return new CoordinateIndex((int) (ap / resolution), (int) (dv / resolution), (int) (ml / resolution));
        }

        /**
         * @param resolution int array of {ap, dv, ml}
         * @return
         */
        public CoordinateIndex toCoorIndex(double[] resolution) {
            if (resolution.length != 3) throw new IllegalArgumentException();
            return new CoordinateIndex((int) (ap / resolution[0]), (int) (dv / resolution[1]), (int) (ml / resolution[2]));
        }
    }

    /**
     * coordinate system in anatomical space.
     *
     * @param ap
     * @param dv
     * @param ml
     */
    public record CoordinateIndex(int ap, int dv, int ml) {
        public Coordinate toCoor(double resolution) {
            return new Coordinate(ap * resolution, dv * resolution, ml * resolution);
        }

        /**
         * @param resolution int array of {ap, dv, ml}
         * @return
         */
        public Coordinate toCoor(double[] resolution) {
            if (resolution.length != 3) throw new IllegalArgumentException();
            return new Coordinate(ap * resolution[0], dv * resolution[1], ml * resolution[2]);
        }
    }

    /**
     * @return {@link AnatomicalSpace.Labels#left} or {@link AnatomicalSpace.Labels#right}
     */
    public AnatomicalSpace.Labels hemisphereFromCoords(Coordinate coor) {
        return hemisphereFromCoords(coor.toCoorIndex(resolution()));
    }

    /**
     * @return {@link AnatomicalSpace.Labels#left} or {@link AnatomicalSpace.Labels#right}
     */
    public AnatomicalSpace.Labels hemisphereFromCoords(CoordinateIndex coor) {
        ImageVolume volume;
        try {
            volume = hemispheres();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var hem = volume.get(coor);
        return switch (hem) {
            case 1 -> AnatomicalSpace.Labels.left;
            case 2 -> AnatomicalSpace.Labels.right;
            default -> throw new RuntimeException();
        };
    }

    public @Nullable Structure structureFromCoords(Coordinate coor) {
        return structureFromCoords(coor.toCoorIndex(resolution()));
    }

    public @Nullable Structure structureFromCoords(CoordinateIndex coor) {
        ImageVolume volume;
        try {
            volume = annotation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var rid = volume.get(coor);
        return structures.get(rid).orElse(null);
    }

    /*=========*
     * slicing *
     *=========*/


}
