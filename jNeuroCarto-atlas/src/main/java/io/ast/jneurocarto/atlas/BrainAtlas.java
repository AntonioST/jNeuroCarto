package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.CoordinateIndex;

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

    public String name() {
        return meta.name;
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

    private final ImageVolume empty = new ImageVolume(0, 0, 0, false);
    private volatile ImageVolume reference = empty;
    private volatile ImageVolume annotation = empty;

    public ImageVolume reference() throws IOException {
        if (reference == empty) {
            var file = root.resolve(REFERENCE_FILENAME);
            synchronized (empty) {
                if (reference == empty) {
                    log.debug("load reference {}", file);
                    reference = ImageVolume.readTiff(file);
                    log.debug("loaded reference");
                }
            }
        }
        return Objects.requireNonNull(reference);
    }

    public ImageVolume annotation() throws IOException {
        if (annotation == empty) {
            var file = root.resolve(ANNOTATION_FILENAME);
            synchronized (empty) {
                if (annotation == empty) {
                    log.debug("load annotation {}", file);
                    annotation = ImageVolume.readTiff(file);
                    log.debug("loaded annotation");
                }
            }
        }
        return Objects.requireNonNull(annotation);
    }

    /*=============*
     * hemispheres *
     *=============*/

    private sealed interface Hemispheres permits HemispheresFromTiff, SymetrocHemispheres, AlwaysNullHemispheres {
        AnatomicalSpace.@Nullable Labels hemisphereFromCoords(CoordinateIndex coor);
    }

    public @Nullable ImageVolume hemispheres() throws IOException {
        return switch (initHemisphere()) {
            case HemispheresFromTiff hem -> hem.hemispheres;
            default -> null;
        };
    }

    /*============*
     * coordinate *
     *============*/

    // TODO how to cooperate with AnatomicalSpace?

    public @Nullable Coordinate getCoordinate(String reference) {
        switch (reference) {
        case "bregma":
            // TODO How do I get bregma coordinate in a system way?
            return new Coordinate(5400, 0, 5700);
        default:
            return null;
        }
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

        int rid;
        try {
            rid = volume.get(coor);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return structures.get(rid).orElse(null);
    }

    private static final class HemispheresFromTiff implements Hemispheres {

        private ImageVolume hemispheres;

        HemispheresFromTiff(ImageVolume hemispheres) {
            this.hemispheres = hemispheres;
        }

        @Override
        public AnatomicalSpace.@Nullable Labels hemisphereFromCoords(CoordinateIndex coor) {
            return switch (hemispheres.get(coor)) {
                case 1 -> AnatomicalSpace.Labels.left;
                case 2 -> AnatomicalSpace.Labels.right;
                default -> null;
            };
        }
    }

    private static final class SymetrocHemispheres implements Hemispheres {
        private final int size;
        private final boolean reversed;

        public SymetrocHemispheres(AnatomicalSpace space) {
            var frontal = space.getAxisIndexOf(AnatomicalSpace.Axes.frontal);
            size = space.shape()[frontal];
            reversed = space.axes()[frontal] < 0;
        }

        @Override
        public AnatomicalSpace.Labels hemisphereFromCoords(CoordinateIndex coor) {
            var ml = coor.ml();

            boolean isLeft = ml < size / 2 + 1;
            /*      !reversed  reversed
            isLeft  left       right
            !isLeft right      left
             */
            return isLeft == reversed ? AnatomicalSpace.Labels.right : AnatomicalSpace.Labels.left;
        }
    }

    private static final class AlwaysNullHemispheres implements Hemispheres {

        @Override
        public AnatomicalSpace.@Nullable Labels hemisphereFromCoords(CoordinateIndex coor) {
            return null;
        }
    }

    /**
     * @return {@link AnatomicalSpace.Labels#left} or {@link AnatomicalSpace.Labels#right}
     */
    public AnatomicalSpace.@Nullable Labels hemisphereFromCoords(Coordinate coor) {
        return hemisphereFromCoords(coor.toCoorIndex(resolution()));
    }

    private volatile @Nullable Hemispheres hemispheres;

    /**
     * @return {@link AnatomicalSpace.Labels#left} or {@link AnatomicalSpace.Labels#right}
     */
    public AnatomicalSpace.@Nullable Labels hemisphereFromCoords(CoordinateIndex coor) {
        Hemispheres hem;
        try {
            hem = initHemisphere();
        } catch (IOException e) {
            log.warn("hemispheres", e);
            hemispheres = hem = new AlwaysNullHemispheres();
        }
        return hem.hemisphereFromCoords(coor);
    }

    private Hemispheres initHemisphere() throws IOException {
        if (hemispheres == null) {
            var file = root.resolve(HEMISPHERES_FILENAME);
            if (meta.symmetric || !Files.exists(file)) {
                hemispheres = new SymetrocHemispheres(space);
            } else {
                synchronized (empty) {
                    if (hemispheres == null) {
                        hemispheres = new HemispheresFromTiff(ImageVolume.readTiff(file));
                    }
                }
            }
        }

        return Objects.requireNonNull(hemispheres);
    }

    /*=========*
     * slicing *
     *=========*/

    public ImageSliceStack reference(ImageSliceStack.Projection projection) throws IOException {
        return new ImageSliceStack(this, reference(), projection);
    }

    public ImageSlice reference(ImageSlice slice) throws IOException {
        return new ImageSliceStack(this, reference(), slice.projection()).sliceAtPlane(slice);
    }

    public ImageSliceStack annotation(ImageSliceStack.Projection projection) throws IOException {
        return new ImageSliceStack(this, annotation(), projection);
    }

    public ImageSlice annotation(ImageSlice slice) throws IOException {
        return new ImageSliceStack(this, annotation(), slice.projection()).sliceAtPlane(slice);
    }
}
