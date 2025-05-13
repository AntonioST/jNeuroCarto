package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Logger log;

    public BrainAtlas(Path root) throws IOException {
        this.root = root;
        log = LoggerFactory.getLogger(BrainAtlas.class);

        Path p;
        meta = BrainAtlasMeta.load(p = root.resolve(METADATA_FILENAME));
        log.debug("loaded {}", p);

        structures = Structures.load(p = root.resolve(STRUCTURES_FILENAME));
        log.debug("loaded {}", p);

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

    public Structures structures() {
        return structures;
    }

    public Structures hierarchy() {
        return structures;
    }

    /*============*
     * image data *
     *============*/

    private volatile @Nullable ImageVolume reference;
    private volatile @Nullable ImageVolume annotation;
    private volatile @Nullable ImageVolume hemispheres;

    public synchronized ImageVolume reference() throws IOException {
        if (reference == null) {
            var file = root.resolve(REFERENCE_FILENAME);
            log.debug("load reference {}", file);
            reference = ImageVolume.readTiff(file);
            log.debug("loaded reference");
        }
        return Objects.requireNonNull(reference);
    }

    public synchronized ImageVolume annotation() throws IOException {
        if (annotation == null) {
            var file = root.resolve(ANNOTATION_FILENAME);
            log.debug("load annotation {}", file);
            annotation = ImageVolume.readTiff(file);
            log.debug("loaded annotation");
        }
        return Objects.requireNonNull(annotation);
    }

    public synchronized ImageVolume hemispheres() throws IOException {
        if (hemispheres == null) {
            // If reference is symmetric generate hemispheres block
            var file = root.resolve(HEMISPHERES_FILENAME);
            log.debug("load hemispheres {}", file);
            if (meta.symmetric) {
                // TODO Are they different really?
                hemispheres = ImageVolume.readTiff(file);
            } else {
                hemispheres = ImageVolume.readTiff(file);
            }
            log.debug("loaded hemispheres");
        }
        return Objects.requireNonNull(hemispheres);
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

    public ImageSlices reference(ImageSlices.Projection projection) throws IOException {
        return new ImageSlices(this, reference(), projection);
    }

    public ImageSlice reference(ImageSlice slice) throws IOException {
        return new ImageSlices(this, reference(), slice.projection()).sliceAtPlane(slice);
    }

    public ImageSlices annotation(ImageSlices.Projection projection) throws IOException {
        return new ImageSlices(this, annotation(), projection);
    }

    public ImageSlice annotation(ImageSlice slice) throws IOException {
        return new ImageSlices(this, annotation(), slice.projection()).sliceAtPlane(slice);
    }
}
