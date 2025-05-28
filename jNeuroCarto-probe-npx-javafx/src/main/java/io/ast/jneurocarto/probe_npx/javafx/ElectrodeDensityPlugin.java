package io.ast.jneurocarto.probe_npx.javafx;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XYPath;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;

@NullMarked
public class ElectrodeDensityPlugin extends InvisibleView implements ProbePlugin<ChannelMap> {

    private ProbeView<?> canvas;
    private InteractionXYPainter painter;
    private XYPath @Nullable [] baseline;
    private XYPath @Nullable [] curves;

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
    protected @Nullable Node setupContent(PluginSetupService service) {
        canvas = service.getProbeView();
        painter = canvas.getForegroundPainter();
        painter.visible.bindBidirectional(visible);
        painter.visible.addListener((_, _, e) -> painter.repaint());

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

            updateXYData(chmap, density);

            repaint();
        }
    }

    private void updateXYData(ChannelMap chmap, double[][] density) {
        var ps = chmap.type().spacePerShank();
        var pc = chmap.type().spacePerColumn();
        var spacing = getSpacing();

        var nShank = density.length;
        var length = density[0].length;

        if (curves != null) painter.removeGraphics(Arrays.asList(curves));
        if (baseline != null) painter.removeGraphics(Arrays.asList(baseline));
        if (curves == null || curves.length < nShank) curves = new XYPath[nShank];
        if (baseline == null || baseline.length < nShank) baseline = new XYPath[nShank];

        for (int shank = 0; shank < nShank; shank++) {
            var path = new XYPath();
            baseline[shank] = path;
            painter.addGraphics(path);

            path.line(Color.BLUE);
            path.linewidth(1);
            path.alpha(0.5);
            var zero = shank * ps + 2 * pc;
            path.addData(zero, 0);
            path.addData(zero, length * spacing);
        }

        for (int shank = 0; shank < nShank; shank++) {
            var path = new XYPath();
            curves[shank] = path;
            painter.addGraphics(path);

            path.line(Color.BLUE);
            path.linewidth(1);
            path.clearData();

            var zero = shank * ps + 2 * pc;
            var curve = density[shank];
            for (int i = 0; i < length; i++) {
                path.addData(zero + curve[i] * ps, i * spacing);
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
