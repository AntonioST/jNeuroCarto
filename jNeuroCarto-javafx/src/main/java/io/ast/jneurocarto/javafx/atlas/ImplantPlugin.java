package io.ast.jneurocarto.javafx.atlas;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.*;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.StateView;

@NullMarked
public class ImplantPlugin implements ProbePlugin<Object>, StateView<ImplantState>, InteractionXYChart.PlottingJob {

    private final ProbeDescription<Object> probe;
    private final AtlasPlugin atlas;
    private @Nullable AtlasReferenceService references;

    private ProbeView<Object> canvas;
    private @Nullable String currentChannelmapCode;
    private @Nullable ShankCoordinate shankCoor;
    private @Nullable AtlasReference implantReference;
    private @Nullable ProbeTransform</*global*/Coordinate, /*reference*/Coordinate> implantTransform;

    private Logger log = LoggerFactory.getLogger(ImplantPlugin.class);

    public ImplantPlugin(ProbeDescription<Object> probe, AtlasPlugin atlas) {
        this.probe = probe;
        this.atlas = atlas;

        Thread.ofVirtual().name("loadAtlasReferencesData").start(() -> {
            references = AtlasReferenceService.loadReferences(atlas);
        });
    }

    @Override
    public String name() {
        return "Probe implant plugin";
    }

    /*============*
     * properties *
     *============*/

    public ObjectProperty<@Nullable ImplantCoordinate> implantCoordinateProperty = new SimpleObjectProperty<>(null);

    {
        implantCoordinateProperty.addListener((_, _, im) -> onImplantCoordinateUpdate(im));
    }

    public @Nullable ImplantCoordinate getImplantcoordinate() {
        return implantCoordinateProperty.get();
    }

    public void setImplantCoordinate(@Nullable ImplantCoordinate coordinate) {
        implantCoordinateProperty.set(coordinate);
    }

    public final BooleanProperty showImplantCoordinateProperty = new SimpleBooleanProperty(true);

    public final boolean isShowImplantCoordinate() {
        return showImplantCoordinateProperty.get();
    }

    public final void setShowImplantcoordinate(boolean value) {
        showImplantCoordinateProperty.set(value);
    }

    public final BooleanProperty showAtlasReferenceProperty = new SimpleBooleanProperty(false);

    public final boolean isShowAtlasReference() {
        return showAtlasReferenceProperty.get();
    }

    public final void setShowAtlasReference(boolean value) {
        showAtlasReferenceProperty.set(value);
    }

    /*=================*
     * state load/save *
     *=================*/

    @Override
    public @Nullable ImplantState getState() {
        var implant = implantCoordinateProperty.get();
        if (implant == null) return null;

        var state = new ImplantState();
        state.ap = implant.ap();
        state.dv = implant.dv();
        state.ml = implant.ml();
        state.shank = implant.s();
        state.rap = implant.rap();
        state.rdv = implant.rdv();
        state.rml = implant.rml();
        state.depth = implant.depth();
        state.reference = implant.reference();
        return state;
    }

    @Override
    public void restoreState(@Nullable ImplantState state) {
        if (state == null) return;

        var implant = new ImplantCoordinate(
            state.ap, state.dv, state.ml, state.shank,
            state.rap, state.rdv, state.rml, state.depth,
            state.reference
        );
        log.debug("restore {}", implant);
        setImplantCoordinate(implant);
    }

    /*==========*
     * UI Setup *
     *==========*/

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        // chart
        this.canvas = (ProbeView<Object>) service.getProbeView();
        canvas.addBackgroundPlotting(this);

        // menu items
        var openImplant = new MenuItem("Edit implant coordinate");
        openImplant.setOnAction(this::openImplantDialog);
        service.addMenuInEdit(openImplant);

        var showImplantCoor = new CheckMenuItem("show implant coordinate");
        showImplantCoor.selectedProperty().bindBidirectional(showImplantCoordinateProperty);
        service.addMenuInView(showImplantCoor);

        //
        implantCoordinateProperty.addListener((_, _, _) -> updateImage());
        showImplantCoordinateProperty.addListener((_, _, _) -> updateImage());
        showAtlasReferenceProperty.addListener((_, _, _) -> updateImage());

        return null;
    }

    /*==============*
     * event handle *
     *==============*/

    @Override
    public void onProbeUpdate(Object chmap, List<ElectrodeDescription> blueprint) {
        var code = probe.channelmapCode(chmap);
        if (code != null && !Objects.equals(code, currentChannelmapCode)) {
            currentChannelmapCode = code;
            shankCoor = probe.getShankCoordinate(code);
        }
    }

    private void openImplantDialog(ActionEvent e) {
        var state = getState();
        if (state == null) {
            state = new ImplantState();
        }
        openImplantDialog(state);
    }

    private void openImplantDialog(ImplantState state) {
        var dialog = new ImplantEditDialog(references, state);
        dialog.implant.addListener((_, _, newState) -> restoreState(newState));
        dialog.show();
    }

    private void onImplantCoordinateUpdate(@Nullable ImplantCoordinate implant) {
        if (implant == null) {
            implantTransform = null;
            implantReference = null;
        } else if (implant.reference() != null) {
            assert references != null;
            var ref = references.getReference(implant.reference());
            if (ref != null) {
                implantTransform = ProbeTransform.create(implant.reference(), ref.coordinate(), ref.flipAP());
                implantReference = ref;
            } else {
                log.warn("reference {} not found", implant.reference());
                implantTransform = null;
                implantReference = null;
            }
        } else {
            implantTransform = ProbeTransform.identify(ProbeTransform.ANATOMICAL);
            implantReference = null;
        }

    }

    /*==========*
     * plotting *
     *==========*/

    @Override
    public double z() {
        return 100;
    }

    private void updateImage() {
        canvas.repaintForeground();
    }

    @Override
    public void draw(GraphicsContext gc) {
        var implant = implantCoordinateProperty.get();
        if (implant == null) return;

        var showImplant = isShowImplantCoordinate();
        var showReference = isShowAtlasReference();
        if (!showImplant && !showReference) return;

        var stack = atlas.getImageSliceStack();
        var image = atlas.getImageSlice();
        if (stack == null || image == null) return;

        var ref = implantReference;
        var pt = implantTransform;
        if (pt == null) return;

        gc.save();
        try {
            // chart -> image -> slice?
            gc.transform(atlas.painter().getChartTransform());

            double dx, dy;
            try {
                var d = gc.getTransform().inverseDeltaTransform(10, 10);
                dx = Math.abs(d.getX());
                dy = Math.abs(d.getY());
            } catch (NonInvertibleTransformException e) {
                dx = dy = 10;
            }

            if (showImplant) {
                System.out.println("dx = " + dx + ", dy = " + dy);
                var coor = implant.insertCoordinate();
                System.out.println("coor = " + coor);
                var point = stack.project(pt.transform(coor));
                System.out.println("point = " + point);
                var plantOffset = Math.abs(image.planeDistance() - point.p());
                System.out.println("plantOffset = " + plantOffset);
                var alpha = Math.max(0, 1 - plantOffset / 5000);
                gc.setGlobalAlpha(alpha);
                gc.setStroke(Color.YELLOW);
                var x = point.x();
                var y = point.y();
                gc.strokeLine(x - dx, y - dy, x + dx, y + dy);
                gc.strokeLine(x - dx, y + dy, x + dx, y - dy);
            }

            if (showReference && ref != null) {
                var coor = ref.coordinate();
                var point = stack.project(pt.transform(coor));
                var plantOffset = Math.abs(image.planeDistance() - point.p());
                var alpha = Math.max(0, 1 - plantOffset / 5000);
                gc.setGlobalAlpha(alpha);
                gc.setStroke(Color.RED);
                var x = point.x();
                var y = point.y();
                gc.strokeLine(x - dx, y - dy, x + dx, y + dy);
                gc.strokeLine(x - dx, y + dy, x + dx, y - dy);
            }
        } finally {
            gc.restore();
        }
    }
}
