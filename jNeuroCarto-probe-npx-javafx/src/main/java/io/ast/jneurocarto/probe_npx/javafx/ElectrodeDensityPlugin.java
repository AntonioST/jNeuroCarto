package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

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

    private static final Affine IDENTIFY = new Affine();
    private @Nullable ProbeView<?> canvas;

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

    /**
     * the pre-allocated array for storing transformed density curves.
     */
    private double @Nullable [][][] curves;

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

            var nShank = density.length;
            var length = density[0].length;

            if (curves == null || curves.length != nShank || curves[0][0].length != length) {
                curves = new double[nShank][2][length];
                for (int i = 0; i < nShank; i++) {
                    curves[i] = new double[2][];
                    curves[i][0] = new double[length];
                    curves[i][1] = new double[length];
                }
            }

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
        var curves = this.curves;
        if (chmap == null || density == null || !isVisible()) return;

        var ps = chmap.type().spacePerShank();
        var pc = chmap.type().spacePerColumn();
        var spacing = getSpacing();

        // Because line width is affect by scaling,
        // we save the transformed positions first,
        // then plot the positions without the transformation.
        var aff = gc.getTransform();
        for (int shank = 0; shank < density.length; shank++) {
            var dos = density[shank];
            var zero = shank * ps + 2 * pc;

            var curve = curves[shank];
            for (int i = 0, length = dos.length; i < length; i++) {
                var p = aff.transform(zero + dos[i] * ps, i * spacing);
                curve[0][i] = p.getX();
                curve[1][i] = p.getY();
            }
        }

        gc.save();
        try {
            gc.setTransform(IDENTIFY);
            gc.setGlobalAlpha(1);
            gc.setStroke(Color.BLUE);
            gc.setLineWidth(1);
            for (int shank = 0; shank < density.length; shank++) {
                var curve = curves[shank];

                gc.beginPath();
                gc.moveTo(curve[0][0], curve[1][0]);
                for (int i = 1, length = curve[0].length; i < length; i++) {
                    gc.lineTo(curve[0][i], curve[1][i]);
                }
                gc.stroke();
            }
        } finally {
            gc.restore();
        }
    }
}
