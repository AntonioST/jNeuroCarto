package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.BrainGlobeDownloader;
import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.config.JsonConfig;
import io.ast.jneurocarto.javafx.app.PluginStateService;

/// A service that read/write the [AtlasReferenceState].
///
/// ### Example config
///
/// Use class [AtlasReferenceState].
///
/// {@snippet lang = "JSON":
///     "AtlasReferences": {
///       "brains" : {
///         "allen_mouse_10um": {
///           "references": {
///             "bregma": [5400, 0, 5700, 1]
///           },
///         },
///         "allen_mouse_25um": {
///           "use" : "allen_mouse_10um"
///         }
///       }
///     }
///}
///
/// In this example, brain "allen_mouse_25um" use the same references as "allen_mouse_10um",
/// so it declared it use "allen_mouse_10um". brain "allen_mouse_10um" has one reference point
/// called "bregma", which located at `(5400, 0, 5700)` (`(AP, DL, ML)` in um) with `AP`-axis flipped.
///
/// ### File Source
///
/// The following files are reading in the order if they are presented,
/// and the latter one overwrite the former one.
///
/// 1. The builtin config put in the classpath (`DefaultAtlasReferences.json`).
/// 2. The global config (user config).
/// 3. The local config (probe config).
@NullMarked
public final class AtlasReferenceService {

    private static volatile @Nullable AtlasReferenceState references;
    private static final Logger log = LoggerFactory.getLogger(AtlasReferenceService.class);
    private String atlas;

    private AtlasReferenceService(String atlas) {
        this.atlas = atlas;
    }

    public static AtlasReferenceService loadReferences(BrainAtlas atlas) {
        return loadReferences(atlas.name());
    }

    public static AtlasReferenceService loadReferences(BrainGlobeDownloader.DownloadResult atlas) {
        return loadReferences(atlas.atlas());
    }

    public static AtlasReferenceService loadReferences(AtlasPlugin plugin) {
        return loadReferences(plugin.atlasName());
    }

    public static AtlasReferenceService loadReferences(String atlas) {
        var references = loadReferences();

        log.debug("chack {}", atlas);
        var data = references.get(atlas);
        if (data == null) {
            log.warn("missing atlas {} references", atlas);
        }

        return new AtlasReferenceService(atlas);
    }

    private static AtlasReferenceState loadReferences() {
        var references = AtlasReferenceService.references;
        if (references == null) {
            synchronized (AtlasReferenceService.class) {
                references = AtlasReferenceService.references;
                if (references == null) {
                    log.debug("loadReferences");

                    var state = new AtlasReferenceState();
                    AtlasReferenceState s;
                    if ((s = loadInternalResource()) != null) {
                        state.brains.putAll(s.brains);
                    }
                    if ((s = PluginStateService.loadGlobalState(AtlasReferenceState.class)) != null) {
                        state.brains.putAll(s.brains);
                    }
                    if ((s = PluginStateService.loadLocalState(AtlasReferenceState.class)) != null) {
                        state.brains.putAll(s.brains);
                    }

                    log.debug("load brain {}", state.brains.keySet());
                    AtlasReferenceService.references = references = state;
                }
            }
        }
        return references;
    }

    private static @Nullable AtlasReferenceState loadInternalResource() {
        var is = AtlasReferenceService.class.getResourceAsStream("DefaultAtlasReferences.json");
        if (is == null) {
            log.warn("loadInternalResource, DefaultAtlasReferences.json not find");
            return null;
        }
        try (is) {
            var config = JsonConfig.load(is);
            return config.get(AtlasReferenceState.class);
        } catch (Exception e) {
            log.warn("loadInternalResource", e);
            return null;
        }
    }

    public void saveReferences() {
        var references = Objects.requireNonNull(AtlasReferenceService.references);
        PluginStateService.saveLocalState(references);
    }

    public List<String> getReferenceList() {
        var references = Objects.requireNonNull(AtlasReferenceService.references);
        var data = references.get(atlas);
        if (data == null) return List.of();
        return new ArrayList<>(data.references.keySet());
    }

    public @Nullable AtlasReference getReference(String name) {
        var references = Objects.requireNonNull(AtlasReferenceService.references);
        return references.get(atlas, name);
    }

    public void addReference(AtlasReference reference) {
        var references = Objects.requireNonNull(AtlasReferenceService.references);
        references.add(reference);
    }

    public void addReference(String name, Coordinate coordinate) {
        addReference(name, coordinate, true);
    }

    public void addReference(String name, Coordinate coordinate, boolean flipAP) {
        Objects.requireNonNull(atlas, "atlas name");
        addReference(new AtlasReference(atlas, name, coordinate, flipAP));
    }
}
