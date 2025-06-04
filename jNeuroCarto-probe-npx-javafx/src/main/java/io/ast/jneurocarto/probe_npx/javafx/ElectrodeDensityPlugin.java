package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;

@NullMarked
public class ElectrodeDensityPlugin extends InvisibleView implements ProbePlugin<ChannelMap> {

    private InteractionXYPainter painter;

    /**
     * ChannelMap cache.
     */
    private @Nullable ChannelMap chmap;

    /**
     * hash code of {@link #chmap}
     */
    private int chmapHash = 0;

    /**
     * the result of {@link ChannelMaps#calculateElectrodeDensity(ChannelMap, double, double)}.
     * Updated only when the {@link #chmap} is updated, via comparing {@link #chmapHash}.
     */
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
    protected void setupChartContent(PluginSetupService service, ProbeView<?> canvas) {
        painter = canvas.getForegroundPainter();
        painter.visible.bindBidirectional(visible);
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

            updateXYData(chmap, density);

            repaint();
        }
    }

    private void updateXYData() {
        var chmap = this.chmap;
        var density = this.density;
        if (chmap == null || density == null) return;
        updateXYData(chmap, density);
    }

    private void updateXYData(ChannelMap chmap, double[][] density) {
        painter.clearGraphics();

        var ps = chmap.type().spacePerShank();
        var pc = chmap.type().spacePerColumn();
        var spacing = getSpacing();

        var nShank = density.length;
        var length = density[0].length;

        for (int shank = 0; shank < nShank; shank++) {
            var zero = shank * ps + 2 * pc;
            painter.lines()
              .z(5)
              .line(Color.BLUE)
              .linewidth(1)
              .alpha(0.5)
              .addPoint(zero, 0)
              .addPoint(zero, length * spacing);
        }

        for (int shank = 0; shank < nShank; shank++) {
            var builder = painter.lines()
              .z(10)
              .line(Color.BLUE)
              .linewidth(1)
              .alpha(1);

            var zero = shank * ps + 2 * pc;
            var curve = density[shank];

            for (int i = 0; i < length; i++) {
                builder.addPoint(zero + curve[i] * ps / 2, i * spacing);
            }
        }
    }

    /*================*
     * curve plotting *
     *================*/

    public void repaint() {
        painter.repaint();
    }
}
