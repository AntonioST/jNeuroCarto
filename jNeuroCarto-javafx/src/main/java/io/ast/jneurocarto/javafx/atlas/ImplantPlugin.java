package io.ast.jneurocarto.javafx.atlas;

import java.util.List;
import java.util.Objects;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ImplantCoordinate;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.ShankCoordinate;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.StateView;

@NullMarked
public class ImplantPlugin implements ProbePlugin<Object>, StateView<ImplantState> {

    private final ProbeDescription<Object> probe;
    private final AtlasPlugin atlas;
    private @Nullable AtlasReferenceService references;

    private ProbeView<Object> canvas;
    private InteractionXYPainter painter;
    private @Nullable String currentChannelmapCode;
    private @Nullable ShankCoordinate shankCoor;
    private @Nullable ImplantCoordinate implant;

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

    /*=================*
     * state load/save *
     *=================*/

    @Override
    public @Nullable ImplantState getState() {
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

        implant = new ImplantCoordinate(
            state.ap, state.dv, state.ml, state.shank,
            state.rap, state.rdv, state.rml, state.depth,
            state.reference
        );
        log.debug("restore {}", implant);
    }

    /*==========*
     * UI Setup *
     *==========*/

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        // chart
        this.canvas = (ProbeView<Object>) service.getProbeView();
        painter = canvas.getForegroundPainter();

        // menu items
        var openImplant = new MenuItem("Edit implant coordinate");
        openImplant.setOnAction(this::openImplantDialog);
        service.addMenuInEdit(openImplant);

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
}
