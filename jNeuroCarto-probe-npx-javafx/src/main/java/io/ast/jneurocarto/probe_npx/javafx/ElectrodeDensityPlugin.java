package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.javafx.app.InteractionXYChart;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;

@NullMarked
public class ElectrodeDensityPlugin extends InvisibleView implements ProbePlugin<ChannelMap>, InteractionXYChart.PlottingJob {

    private @Nullable ProbeView<?> canvas;
    private @Nullable ChannelMap chmap;
    private int chmapHash = 0;
    private double @Nullable [][] density;
    private final Logger log = LoggerFactory.getLogger(ElectrodeDensityPlugin.class);

    public ElectrodeDensityPlugin() {
    }

    @Override
    public String name() {
        return "Electrode density";
    }

    @Override
    public String description() {
        return "show electrode density beside probe shanks.";
    }

    /*============*
     * properties *
     *============*/

    public final DoubleProperty spacing = new SimpleDoubleProperty(5);

    public final double getSpacing() {
        return spacing.get();
    }

    public final void setSpacing(double value) {
        if (value <= 0) throw new IllegalArgumentException();
        spacing.set(value);
    }

    public final DoubleProperty smooth = new SimpleDoubleProperty(25);

    public final double getSmooth() {
        return smooth.get();
    }

    public final void setSmooth(double value) {
        if (value <= 0) throw new IllegalArgumentException();
        smooth.set(value);
    }

    /*===========*
     * UI Layout *
     *===========*/

    @Override
    protected @Nullable Node setupContent(PluginSetupService service) {
        canvas = service.getProbeView();
        canvas.addForegroundPlotting(this);

        return null;
    }

    /*=============*
     * probe event *
     *=============*/

    @Override
    public void onProbeUpdate(ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        int hash = Objects.hashCode(chmap);
        if (this.chmap == null || chmapHash != hash) {
            this.chmap = chmap;
            chmapHash = hash;

            log.debug("update electrode density");
            density = ChannelMaps.calculateElectrodeDensity(chmap, getSpacing(), getSmooth());

            var canvas = this.canvas;
            if (canvas != null) canvas.repaintForeground();
        }
    }

    /*================*
     * curve plotting *
     *================*/

    @Override
    public void draw(GraphicsContext gc) {
        var chmap = this.chmap;
        var density = this.density;
        if (chmap == null || density == null || !isVisible()) return;

        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2);

        var ps = chmap.type().spacePerShank();
        var pc = chmap.type().spacePerColumn();
        var spacing = getSpacing();

        for (int shank = 0; shank < density.length; shank++) {
            var dos = density[shank];
            var zero = shank * ps + pc;

            gc.beginPath();
            gc.moveTo(zero, 0);
            for (int i = 0, length = dos.length; i < length; i++) {
                gc.lineTo(zero + dos[i] * pc, i * spacing);
            }
            System.out.println();
            gc.stroke();
        }
    }
}
