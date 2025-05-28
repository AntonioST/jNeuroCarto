package io.ast.jneurocarto.probe_npx.javafx;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.config.Repository;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.chart.Colormap;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XYMatrix;
import io.ast.jneurocarto.javafx.view.AbstractImagePlugin;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.StateView;
import io.ast.jneurocarto.probe_npx.ChannelMap;

public class DataVisualizePlugin extends AbstractImagePlugin implements ProbePlugin<ChannelMap>, StateView<DataVisualizeState> {

    private InteractionXYPainter foreground;
    private XYMatrix[] matrix;

    /**
     * ChannelMap cache.
     */
    private @Nullable ChannelMap chmap;

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

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");
        var ret = super.setup(service);

        foreground = canvas.getForegroundPainter();
        colormapProperty.addListener((_, _, cmap) -> {
            var matrix = this.matrix;
            if (matrix != null) {
                var cm = Colormap.of(cmap);
                for (XYMatrix m : matrix) {
                    if (m != null) {
                        m.colormap(cm);
                    }
                }
            }
            foreground.repaint();
        });
        foreground.visible.bindBidirectional(showImageProperty);
        foreground.visible.addListener((_, _, e) -> foreground.repaint());

        return ret;
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
        var file = fileProperty.get();
        if (file == null) return;

        double[] data;

        var toolkit = BlueprintAppToolkit.newToolkit();

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

        updateDataImage(data);
        foreground.repaint();
    }

    protected void onClearData(ActionEvent e) {
        if (matrix != null) foreground.clearGraphics();
        foreground.repaint();
    }

    /*================*
     * image painting *
     *================*/

    @Override
    public void onProbeUpdate(ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        this.chmap = chmap;
    }

    private void updateDataImage(double[] data) {
        foreground.clearGraphics();

        var chmap = this.chmap;
        if (chmap == null) return;

        var type = chmap.type();
        var nShank = type.nShank();
        if (matrix == null || matrix.length < nShank) matrix = new XYMatrix[nShank];

        var toolkit = BlueprintAppToolkit.newToolkit();
        var cmap = colormapProperty.get();

        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();
        var w = pc * type.nColumnPerShank();
        var nr = type.nRowPerShank();
        var h = pr * nr;

        for (int shank = 0; shank < nShank; shank++) {
            matrix[shank] = foreground.imshow()
              .extent(ps * shank - w - pc, 0, w, h)
              .colormap(cmap)
              .z(-1)
              .graphics();
        }

        toolkit.stream(data).forEach(e -> {
            matrix[e.s()].addData((double) e.x() / pc, (double) e.y() / pr, e.v());
        });
    }

    @Override
    public void repaint() {
        foreground.repaint();
    }
}
