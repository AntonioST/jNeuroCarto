package io.ast.jneurocarto.javafx.atlas;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.*;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.view.Plugin;
import io.ast.jneurocarto.javafx.view.ProbeUpdateHandler;
import io.ast.jneurocarto.javafx.view.StateView;

@NullMarked
public class ImplantPlugin implements Plugin, ProbeUpdateHandler<Object>, StateView<ImplantState>, InteractionXYChart.PlottingJob {

    private final ProbeDescription<Object> probe;
    private final AtlasPlugin atlas;
    private final AtlasReferenceService references;

    private ProbeView<Object> canvas;
    private @Nullable String currentChannelmapCode;
    private @Nullable ShankCoordinate shankCoor;
    private @Nullable AtlasReference implantReference;
    private @Nullable ProbeTransform</*global*/Coordinate, /*reference*/Coordinate> implantTransform;

    private Logger log = LoggerFactory.getLogger(ImplantPlugin.class);

    public ImplantPlugin(ProbeDescription<Object> probe, AtlasPlugin atlas) {
        this.probe = probe;
        this.atlas = atlas;

        references = atlas.getReferencesService();
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

    public @Nullable ImplantCoordinate getImplantCoordinate() {
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

        return getState(implant);
    }

    private ImplantState getState(ImplantCoordinate implant) {
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
        canvas.addEventFilter(AtlasUpdateEvent.POSITION, this::onAtlasImagePositionChanged);

        // edit menu items
        var openImplant = new MenuItem("Edit implant coordinate");
        openImplant.setOnAction(this::openImplantDialog);
        service.addMenuInEdit(openImplant);

        // view menu items
        var showImplantCoor = new CheckMenuItem("show implant coordinate");
        showImplantCoor.selectedProperty().bindBidirectional(showImplantCoordinateProperty);

        var focusImplantCoor = new MenuItem("focus on implant coordinate");
        focusImplantCoor.setOnAction(this::onFocusImplantCoordinate);

        service.addMenuInView(showImplantCoor, focusImplantCoor);

        //
        implantCoordinateProperty.addListener((_, _, _) -> updateImage());
        showImplantCoordinateProperty.addListener((_, _, _) -> updateImage());
        showAtlasReferenceProperty.addListener((_, _, _) -> updateImage());

        return null;
    }

    /*==============*
     * event handle *
     *==============*/

    private @Nullable ImplantEditDialog dialog;

    @Override
    public void onProbeUpdate(Object chmap, List<ElectrodeDescription> blueprint) {
        var code = probe.channelmapCode(chmap);
        if (code != null && !Objects.equals(code, currentChannelmapCode)) {
            currentChannelmapCode = code;
            shankCoor = probe.getShankCoordinate(code);
        }
    }

    private void onAtlasImagePositionChanged(AtlasUpdateEvent e) {
        var dialog = this.dialog;
        if (dialog != null && dialog.isShowing()) {
            var implant = newImplantCoordinate();
            if (implant != null) {
                dialog.updateImplantState(getState(implant));
            }
        } else {
            this.dialog = null;
        }
    }

    private void openImplantDialog(ActionEvent e) {
        var state = getState();
        if (state == null) {
            state = new ImplantState();
            if (implantReference != null) {
                state.reference = implantReference.name();
            } else {
                var ref = atlas.reference.get();
                if (ref != null) {
                    state.reference = ref.name();
                }
            }
        }
        openImplantDialog(state);
    }

    private void openImplantDialog(ImplantState state) {
        var dialog = new ImplantEditDialog(references, state);
        dialog.implant.addListener((_, _, newState) -> {
            restoreState(newState);
            if (newState != null) {
                var implant = implantCoordinateProperty.get();
                if (implant != null) focusImplantCoordinate(implant);
            }
        });
        dialog.show();
        this.dialog = dialog;
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

    private void onFocusImplantCoordinate(ActionEvent e) {
        var implant = implantCoordinateProperty.get();
        if (implant != null) focusImplantCoordinate(implant);
    }

    /*===========================*
     * implant coordinate handle *
     *===========================*/

    private ImplantCoordinate toGlobalCoordinate(ImplantCoordinate coordinate) {
        var reference = coordinate.reference();
        if (reference != null) {
            var ref = references.getReference(reference);
            if (ref == null) throw new RuntimeException("unknown reference : " + reference);
            coordinate = coordinate.changeReference(ref.getTransform().inverted());
        }
        return coordinate;
    }

    private ImplantCoordinate toReferenceCoordinate(ImplantCoordinate coordinate, @Nullable String reference) {
        if (coordinate.reference() != null) {
            coordinate = toGlobalCoordinate(coordinate);
        }

        if (reference == null) return coordinate;

        var ref = references.getReference(reference);
        if (ref == null) throw new RuntimeException("unknown reference : " + reference);
        coordinate = coordinate.changeReference(ref.getTransform());
        return coordinate;
    }

    public @Nullable ImplantCoordinate newImplantCoordinate() {
        var implant = getImplantCoordinate();
        var s = implant == null ? 0 : implant.s();
        var r = implant != null ? implant.reference() : atlas.getAtlasReferenceName();
        return newImplantCoordinate(s, r);
    }

    public @Nullable ImplantCoordinate newImplantCoordinate(int shank, @Nullable String reference) {
        var stack = atlas.getImageSliceStack();
        var image = atlas.getImageSlice();
        if (stack == null || image == null) return null;

        ProbeCoordinate probe;
        if (shankCoor == null) {
            probe = new ProbeCoordinate(0, 0, 0);
        } else {
            probe = shankCoor.apply(shank);
        }

        var tsp = atlas.getChartTransform();
        var tcs = atlas.getSliceTransform();
        assert tcs != null;
        var tpc = tcs.compose(tsp).inverted();

        var tip = tpc.transform(probe);
        var dxy = tpc.deltaTransform(0, 1); // 1 depth = dxy = (dap,ddv,dml)
        // depth * ddv = tip.dv
        var depth = Math.abs(tip.dv() / dxy.getY());

        var rot = stack.offset2Angle(image.dw(), image.dh(), atlas.painter().r());

        var implant = new ImplantCoordinate(
            tip.ap() - depth * dxy.getX(),
            tip.dv() - depth * dxy.getY(),
            tip.ml() - depth * dxy.getZ(),
            shank,
            rot.ap(), rot.dv(), rot.ml(),
            Math.abs(depth),
            null
        );

        return toReferenceCoordinate(implant, reference);
    }

    public void focusImplantCoordinate(ImplantCoordinate implant) {
        var stack = atlas.getImageSliceStack();
        if (stack == null) return;

        log.debug("focus {}", implant);
        implant = toGlobalCoordinate(implant);
        log.trace("focus {}", implant);

        var coor = implant.insertCoordinate();
        log.trace("insert at {}", coor);

        var drwh = stack.angle2Offset(implant.rotation());
        log.trace("rotate {}", drwh);

        var slice = stack.sliceAtPlane(coor).withOffset(drwh.x(), drwh.y());

        atlas.setImageSlice(slice, drwh.p());

        ProbeCoordinate probe;
        if (shankCoor == null) {
            probe = new ProbeCoordinate(0, 0, 0);
        } else {
            probe = shankCoor.apply(implant.s());
        }

        probe = new ProbeCoordinate(0, probe.x(), probe.y() + implant.depth());
        log.trace("anchor to {}", probe);
        atlas.anchorImageTo(atlas.project(coor), new Point2D(probe.x(), probe.y()));
    }

    /*==========*
     * plotting *
     *==========*/

    private static final Affine IDENTIFY = new Affine();

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
            var aff = gc.getTransform();
            gc.setTransform(IDENTIFY);
            gc.setLineWidth(2);

            if (showImplant) {
                var coor = implant.insertCoordinate();
                var point = stack.project(pt.transform(coor));
                var plantOffset = Math.abs(image.planeLength() - point.p());
                var alpha = Math.max(0, 1 - plantOffset / 2000);
                gc.setGlobalAlpha(alpha);
                gc.setStroke(Color.GREEN);
                var p = aff.transform(point.x(), point.y());
                var x = p.getX();
                var y = p.getY();
                gc.strokeLine(x - 5, y - 5, x + 5, y + 5);
                gc.strokeLine(x - 5, y + 5, x + 5, y - 5);
            }

            if (showReference && ref != null) {
                var coor = ref.coordinate();
                var point = stack.project(pt.transform(coor));
                var plantOffset = Math.abs(image.planeLength() - point.p());
                var alpha = Math.max(0, 1 - plantOffset / 2000);
                gc.setGlobalAlpha(alpha);
                gc.setStroke(Color.RED);
                var p = aff.transform(point.x(), point.y());
                var x = p.getX();
                var y = p.getY();
                gc.strokeLine(x - 5, y, x + 5, y);
                gc.strokeLine(x, y + 5, x, y - 5);
            }
        } finally {
            gc.restore();
        }
    }
}
