package io.ast.jneurocarto.probe_npx.javafx;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.FileChooser;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.config.Repository;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XYMatrix;
import io.ast.jneurocarto.javafx.view.AbstractImagePlugin;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;

public class DataVisualizePlugin extends AbstractImagePlugin implements ProbePlugin<ChannelMap> {

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

    public final String getString() {
        return colormapProperty.get();
    }

    public final void setString(String colormap) {
        colormapProperty.set(colormap);
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
                for (XYMatrix m : matrix) {
                    if (m != null) {
                        m.colormap(cmap);
                    }
                }
            }
            foreground.repaint();
        });

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

        try {
            data = BlueprintAppToolkit.newToolkit().loadBlueprintData(file);
        } catch (IOException ex) {
            log.warn("loadBlueprintData", ex);
            return;
        }

        updateDataImage(data);
    }

    protected void onClearData(ActionEvent e) {
        if (matrix != null) foreground.removeGraphics(Arrays.asList(matrix));
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
        if (matrix != null) foreground.removeGraphics(Arrays.asList(matrix));

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
            var m = new XYMatrix();
            foreground.addGraphics(m);
            matrix[shank] = m;

            m.x(ps * shank - w);
            m.y(0);
            m.w(w);
            m.h(h);
            m.colormap(cmap);
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
