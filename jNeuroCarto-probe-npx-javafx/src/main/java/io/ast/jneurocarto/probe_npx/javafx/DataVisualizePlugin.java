package io.ast.jneurocarto.probe_npx.javafx;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.config.Repository;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.colormap.Colormap;
import io.ast.jneurocarto.javafx.chart.colormap.LinearColormap;
import io.ast.jneurocarto.javafx.chart.data.XYMatrix;
import io.ast.jneurocarto.javafx.chart.data.XYSeries;
import io.ast.jneurocarto.javafx.utils.ColormapChooseDialog;
import io.ast.jneurocarto.javafx.utils.IOAction;
import io.ast.jneurocarto.javafx.view.AbstractImagePlugin;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.StateView;
import io.ast.jneurocarto.probe_npx.ChannelMap;

public class DataVisualizePlugin extends AbstractImagePlugin implements ProbePlugin<ChannelMap>, StateView<DataVisualizeState> {

    /**
     * ChannelMap cache.
     */
    private @Nullable ChannelMap chmap;
    private double[] data;

    private final Logger log = LoggerFactory.getLogger(DataVisualizePlugin.class);

    public DataVisualizePlugin(Repository repository) {
        super(repository);
    }

    @Override
    public String name() {
        return "Data";
    }

    /*============*
     * properties *
     *============*/

    private @Nullable LinearColormap colormap;
    public final StringProperty colormapProperty = new SimpleStringProperty("plasma");

    public final String getColormap() {
        return colormapProperty.get();
    }

    public final void setColormap(String colormap) {
        colormapProperty.set(colormap);
    }

    private final IntegerProperty interpolateProperty = new SimpleIntegerProperty(1);

    public final int getInterpolate() {
        return interpolateProperty.get();
    }

    public final void setInterpolate(int value) {
        if (value % 2 != 1) throw new IllegalArgumentException();
        interpolateProperty.set(value);
    }

    /*=================*
     * state load/save *
     *=================*/

    @Override
    public @Nullable DataVisualizeState getState() {
        var file = fileProperty.get();

        var state = new DataVisualizeState();
        state.filePath = file == null ? null : file.toAbsolutePath().toString();
        state.colormap = colormapProperty.get();
        state.interpolation = interpolateProperty.get();

        return state;
    }

    @Override
    public void restoreState(@Nullable DataVisualizeState state) {
        if (state == null) return;

        setFile(state.filePath);
        var colormap = state.colormap;
        if (colormap != null) colormapProperty.set(colormap);
        setInterpolate(state.interpolation);
    }

    /*===========*
     * UI layout *
     *===========*/

    private InteractionXYPainter foreground;

    @Override
    protected void setupChartContent(PluginSetupService service, ProbeView<?> canvas) {
        super.setupChartContent(service, canvas);

        foreground = canvas.getForegroundPainter();
        colormapProperty.addListener((_, _, _) -> {
            if (data != null) {
                updateDataImage(data);
                foreground.repaint();
            }
        });
        foreground.visible.bindBidirectional(showImageProperty);
        foreground.visible.addListener((_, _, e) -> foreground.repaint());
    }

    @Override
    protected void setupMenuItems(PluginSetupService service) {
        var setColormap = new MenuItem("Set data colormap");
        setColormap.setOnAction(this::onSetColormap);
        service.addMenuInView(setColormap);
    }

    @Override
    protected void onOpenData(ActionEvent e) {
        var ext = new FileChooser.ExtensionFilter("data file", "*.npy", "*.csv", "*.tsv");
        openDataFileDialog("Open data file", ext).ifPresent(path -> {
            if (Files.isRegularFile(path)) {
                setFile(path);
            }
        });
    }

    protected void onDrawData(ActionEvent e) {
        updateDataImage();
    }

    protected void onClearData(ActionEvent e) {
        clearData();
    }

    private void onSetColormap(ActionEvent e) {
        log.debug("onSetColormap");
        var oldColormap = this.colormap;
        var initColormap = oldColormap != null ? oldColormap : Colormap.of(colormapProperty.get());

        var dialog = new ColormapChooseDialog("Data Visualize", initColormap);
        dialog.colormapProperty.addListener((_, _, colormap) -> {
            log.debug("onSetColormap {}", colormap);
            this.colormap = colormap;
            if (data != null) {
                updateDataImage(data);
                foreground.repaint();
            }
        });

        dialog.showAndWait().ifPresent(r -> {
            if (r.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                log.debug("onSetColormap cancel");
                this.colormap = oldColormap;
                if (data != null) {
                    updateDataImage(data);
                    foreground.repaint();
                }
            }
        });
    }

    /*================*
     * image painting *
     *================*/

    @Override
    public void onProbeUpdate(ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        this.chmap = chmap;
    }

    public void clearData() {
        foreground.clearGraphics();
        foreground.repaint();
    }

    public void updateDataImage() {
        var file = fileProperty.get();
        if (file == null) return;

        var toolkit = BlueprintAppToolkit.newToolkit();

        IOAction.measure(log, "load numpy data", () -> {
            double[] data;

            try {
                data = toolkit.loadBlueprintData(file);
            } catch (IOException ex) {
                log.warn("loadBlueprintData", ex);
                return;
            }

            var kernal = interpolateProperty.get();
            if (kernal > 1) {
                data = toolkit.interpolateNaN(data, kernal, BlueprintToolkit.InterpolateMethod.mean);
            }

            var finalData = data;
            Platform.runLater(() -> {
                updateDataImage(finalData);
                foreground.repaint();
            });
        });
    }

    public void updateDataImage(double[] data) {
        this.data = data;
        foreground.clearGraphics();

        var chmap = this.chmap;
        if (chmap == null) return;

        var type = chmap.type();
        var nShank = type.nShank();
        var matrix = new XYMatrix[nShank];

        var toolkit = BlueprintAppToolkit.newToolkit();

        LinearColormap colormap;
        var updateNormalize = false;
        if (this.colormap != null) {
            colormap = this.colormap;
        } else {
            colormap = Colormap.of(colormapProperty.get());
            updateNormalize = true;
        }

        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();
        var w = pc * type.nColumnPerShank();
        var nr = type.nRowPerShank();
        var h = pr * nr;

        for (int shank = 0; shank < nShank; shank++) {
            matrix[shank] = foreground.imshow()
              .extent(ps * shank - w - pc, 0, w, h)
              .colormap(colormap)
              .z(-1)
              .graphics();
        }

        toolkit.stream(data).forEach(e -> {
            matrix[e.s()].addData((double) e.x() / pc, (double) e.y() / pr, e.v());
        });

        if (updateNormalize) {
            var norm = XYSeries.renormalize(matrix);
            this.colormap = colormap.withNormalize(norm);
        }
    }

    @Override
    public void repaint() {
        foreground.repaint();
    }
}
