package io.ast.jneurocarto.javafx.atlas;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ImplantCoordinate;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.ShankCoordinate;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.javafx.view.StateView;

@NullMarked
public class ImplantPlugin extends InvisibleView implements ProbePlugin<Object>, StateView<ImplantState> {

    private final ProbeDescription<Object> probe;
    private ProbeView<Object> canvas;
    private InteractionXYPainter painter;
    private @Nullable String currentChannelmapCode;
    private @Nullable ShankCoordinate shankCoor;
    private @Nullable ImplantCoordinate implant;

    public ImplantPlugin(ProbeDescription<Object> probe) {
        this.probe = probe;
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
    }

    /*==========*
     * UI Setup *
     *==========*/

    @Override
    protected void setupChartContent(PluginSetupService service, ProbeView<?> canvas) {
        this.canvas = (ProbeView<Object>) canvas;
        painter = canvas.getForegroundPainter();
    }

    /*==============*
     * event handle *
     *==============*/

    @Override
    public void onProbeUpdate(Object chmap, List<ElectrodeDescription> blueprint) {
        var code = probe.channelmapCode(chmap);
        if (code != null && !Objects.equals(code, currentChannelmapCode)) {
            shankCoor = probe.getShankCoordinate(code);
        }
    }
}
