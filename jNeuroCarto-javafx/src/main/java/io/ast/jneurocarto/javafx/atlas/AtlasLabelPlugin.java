package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.PluginStateService;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.StateView;

@NullMarked
public class AtlasLabelPlugin extends InvisibleView implements StateView<AtlasLabelViewState> {

    private @Nullable AtlasPlugin atlas;
    private @Nullable AtlasReferenceState references;
    private InteractionXYPainter foreground;
    private final List<CoordinateLabel> labels = new ArrayList<>();

    private final Logger log = LoggerFactory.getLogger(AtlasLabelPlugin.class);

    @Override
    public String name() {
        return "Atlas labeling";
    }

    /*============*
     * Properties *
     *============*/

    public final BooleanProperty showLabels = new SimpleBooleanProperty(true);

    public boolean isShowLabels() {
        return showLabels.get();
    }

    public void setShowLabels(boolean value) {
        showLabels.set(value);
    }

    /*=================*
     * state load/save *
     *=================*/

    @Override
    public @Nullable AtlasLabelViewState getState() {
        if (labels.isEmpty()) {
            log.debug("save nothing");
            return null;
        }

        log.debug("save");
        return CoordinateLabel.newState(labels);
    }

    @Override
    public void restoreState(@Nullable AtlasLabelViewState state) {
        if (state == null) {
            log.debug("restore nothing");
            return;
        }

        log.debug("restore");
        labels.addAll(CoordinateLabel.getAll(state));
    }

    /*==========================*
     * Reference initialization *
     *==========================*/

    private void loadReferences() {
        log.debug("loadReferences");
        if (atlas == null) {
            log.warn("AtlasPlugin is not loaded");
            return;
        }

        var state = new AtlasReferenceState();
        AtlasReferenceState s;
        if ((s = PluginStateService.loadGlobalState(AtlasReferenceState.class)) != null) {
            state.references.putAll(s.references);
        }
        if ((s = PluginStateService.loadLocalState(AtlasReferenceState.class)) != null) {
            state.references.putAll(s.references);
        }

        references = state;

        var data = references.get(atlas.atlasName());
        if (data == null) {
            log.warn("missing atlas {} references", atlas.atlasName());
        }
    }

    private void saveReferences() {
        if (references != null) {
            PluginStateService.saveLocalState(references);
        }
    }

    public void addReference(AtlasReference reference) {
        if (references == null) {
            references = new AtlasReferenceState();
        }
        references.add(reference);
    }

    public void addReference(String name, Coordinate coordinate) {
        addReference(name, coordinate, true);
    }

    public void addReference(String name, Coordinate coordinate, boolean flipAP) {
        var atlas = this.atlas;
        if (atlas == null) throw new RuntimeException("AtlasPlugin is not loaded");
        addReference(new AtlasReference(atlas.atlasName(), name, coordinate, flipAP));
    }

    /*===========*
     * UI layout *
     *===========*/

    private ChoiceBox<CoordinateLabel.LabelPositionKind> labelKind;
    private TextField labelText;
    private Tooltip labelTextTooltip;

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");
        atlas = service.getPlugin(AtlasPlugin.class);
        loadReferences();

        var view = service.getProbeView();
        foreground = view.getForegroundPainter();

        var ret = super.setup(service);

        view.setOnDataTouch(this::onDataTouch);
        view.setOnDataSelect(this::onDataSelect);

        return ret;
    }

    @Override
    protected HBox setupHeading(PluginSetupService service) {
        var layout = super.setupHeading(service);

        var showLabelSwitch = new CheckBox("Show labels");
        showLabelSwitch.selectedProperty().bindBidirectional(showLabels);
        showLabelSwitch.selectedProperty().bindBidirectional(foreground.visible);

        return layout;
    }

    @Override
    protected Node setupContent(PluginSetupService service) {
        labelKind = new ChoiceBox<>();
        labelKind.getItems().addAll(Arrays.asList(CoordinateLabel.LabelPositionKind.values()));
        labelKind.setOnAction(this::onLabelKindSelect);
        labelKind.setConverter(new StringConverter<>() {
            @Override
            public String toString(CoordinateLabel.LabelPositionKind object) {
                return /*object == null ? "" :*/ object.kind;
            }

            @Override
            public CoordinateLabel.LabelPositionKind fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });

        labelText = new TextField();
        labelText.setOnAction(this::onLabelAdd);

        labelTextTooltip = new Tooltip("");
        Tooltip.install(labelText, labelTextTooltip);

        var layout = new HBox(labelText, labelText);
        layout.setSpacing(5);
        HBox.setHgrow(labelText, Priority.ALWAYS);

        return layout;
    }

    /*==============*
     * event handle *
     *==============*/

    private void onLabelKindSelect(ActionEvent e) {
        var text = switch (labelKind.getValue()) {
            case atlas -> "AP, DV, ML";
            case reference -> "AP, DV, ML[, Reference]";
            case slice -> "[Plane], X, Y";
            case chart, probe -> "X, Y";
        };
        labelTextTooltip.setText(text);
    }

    private void onLabelAdd(ActionEvent e) {

    }

    public void onDataTouch(InteractionXYChart.DataTouchEvent e) {
    }

    public void onDataSelect(InteractionXYChart.DataSelectEvent e) {
    }
}

