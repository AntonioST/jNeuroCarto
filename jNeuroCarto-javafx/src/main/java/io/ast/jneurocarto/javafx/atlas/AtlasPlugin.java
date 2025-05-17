package io.ast.jneurocarto.javafx.atlas;

import java.io.IOException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.*;
import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.javafx.app.LogMessageService;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.utils.IOAction;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.Plugin;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

@NullMarked
public class AtlasPlugin extends InvisibleView implements Plugin {

    private final CartoConfig config;
    private BrainGlobeDownloader.DownloadResult download;
    private @Nullable BrainAtlas brain;
    private @Nullable ImageVolume volume;
    private ProbeView<?> canvas;
    private SlicePainter painter;

    private ImageSliceStack.@Nullable Projection currentProjection;
    private @Nullable ImageSliceStack images;
    private @Nullable ImageSlice image;

    private final Logger log = LoggerFactory.getLogger(AtlasPlugin.class);

    public AtlasPlugin(CartoConfig config) {
        this.config = config;
        download = AtlasBrainService.loadAtlas(config);
    }

    @Override
    public String name() {
        return "Brain Atlas";
    }

    @Override
    public String description() {
        return "showing brain atlas in background";
    }

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        checkBrainAtlas();

        canvas = service.getProbeView();
        painter = new SlicePainter();

        var visibleSwitch = newInvisibleSwitch(name());

        var layout = initLayout();
        bindInvisibleNode(layout);
        setupMenuViewItem(service, name());

        var root = new VBox(
          visibleSwitch,
          layout
        );

        return root;
    }

    /*===========================*
     * BrainAtlas initialization *
     *===========================*/

    private void checkBrainAtlas() {
        if (!download.hasError() || download.isDownloaded()) {
            try {
                brain = download.get();
                loadBrainAtlasImageVolume();
            } catch (IOException e) {
                LogMessageService.printMessage("fail open atlas brain.");
                log.warn("checkBrainAtlas", e);
            }

        } else if (download.isDownloading()) {
            LogMessageService.printMessage("atlas brain is downloading.");

            download.waitDownloadCompleted().whenComplete((result, error) -> {
                if (error != null) {
                    LogMessageService.printMessage("during waiting atlas brain downloading.");
                    log.warn("waitDownloadCompleted", error);
                } else if (result != null) {
                    download = result;
                    checkBrainAtlas();
                }
            });
        } else if (download.isDownloadFailed()) {
            LogMessageService.printMessage("fail download atlas brain.");
        } else {
            LogMessageService.printMessage("fail download atlas brain.");
            log.warn("checkBrainAtlas", download.error());
        }
    }

    private void loadBrainAtlasImageVolume() {
        var brain = this.brain;
        if (brain == null) return;

        IOAction.measure(log, "load reference", () -> {
            volume = brain.reference();
        });
        IOAction.measure(log, "pre load annotation", brain::annotation);
        IOAction.measure(log, "pre load hemispheres", brain::hemispheres);
    }

    /*===========*
     * UI layout *
     *===========*/

    private Node initLayout() {
        var layout = new VBox(
          new Label("content")
        );
        return layout;
    }
}
