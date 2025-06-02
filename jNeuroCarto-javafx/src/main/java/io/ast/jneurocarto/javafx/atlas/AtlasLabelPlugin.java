package io.ast.jneurocarto.javafx.atlas;

import java.util.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.ImageSliceStack;
import io.ast.jneurocarto.atlas.SliceCoordinate;
import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.ProbeCoordinate;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.PluginStateService;
import io.ast.jneurocarto.javafx.atlas.CoordinateLabel.*;
import io.ast.jneurocarto.javafx.chart.*;
import io.ast.jneurocarto.javafx.script.BlueprintScriptCallable.Parameter;
import io.ast.jneurocarto.javafx.utils.FormattedTextField;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.StateView;

import static io.ast.jneurocarto.javafx.script.BlueprintScriptHandles.pairScriptArguments;
import static io.ast.jneurocarto.javafx.script.BlueprintScriptHandles.parseScriptInputLine;

@NullMarked
public class AtlasLabelPlugin extends InvisibleView implements StateView<AtlasLabelViewState> {

    private record XYLabel(CoordinateLabel label, XY data) {
    }

    private @Nullable AtlasPlugin atlas;
    private @Nullable AtlasReferenceState references;
    private InteractionXYPainter foreground;
    private XYText graphics;
    private DiscreteColormap colormap;
    private final Map<String, Integer> colorMapping = new HashMap<>();
    private final List<XYLabel> labels = new ArrayList<>();

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
        return CoordinateLabel.newState(labels.stream().map(XYLabel::label).toList());
    }

    @Override
    public void restoreState(@Nullable AtlasLabelViewState state) {
        if (state == null) {
            log.debug("restore nothing");
            return;
        }

        log.debug("restore");
        for (var label : CoordinateLabel.getAll(state)) {
            addLabel(label);
        }
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

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");
        atlas = service.getPlugin(AtlasPlugin.class);
        loadReferences();

        var view = service.getProbeView();
        foreground = view.getForegroundPainter();
        graphics = foreground.text()
            .colormap(colormap = new DiscreteColormap())
            .graphics();
        colormap.addColor(0, Color.BLACK);
        colorMapping.put("black", 0);

        var ret = super.setup(service);

        view.addEventFilter(InteractionXYChart.DataTouchEvent.DATA_TOUCH, this::onDataTouch);
        view.addEventFilter(InteractionXYChart.DataSelectEvent.DATA_SELECT, this::onDataSelect);
        // TODO label dragging

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
        FormattedTextField.install(labelText, this::validateLabelInput);
        labelText.setOnAction(this::onLabelAdd);

        var layout = new HBox(labelKind, labelText);
        layout.setSpacing(5);
        HBox.setHgrow(labelText, Priority.ALWAYS);

        return layout;
    }

    /*==============*
     * event handle *
     *==============*/

    private void onLabelAdd(ActionEvent e) {
        var kind = labelKind.getValue();
        var text = labelText.getText();
        evalAndAddLabel(kind, text);
    }

    private void onDataTouch(InteractionXYChart.DataTouchEvent e) {
        var xy = graphics.touch(e.point);
        if (xy != null) {
            var label = getLabel(xy);
            if (label != null) {
                onLabelTouch(e, label);
            }
        }
    }

    private void onDataSelect(InteractionXYChart.DataSelectEvent e) {
        var selected = graphics.touch(e.bounds).stream()
            .map(this::getLabel)
            .filter(Objects::nonNull)
            .toList();

        if (!selected.isEmpty()) {
            onLabelSelect(e, selected);
        }
    }


    private void onLabelTouch(InteractionXYChart.DataTouchEvent e, XYLabel label) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            focusOnLabel(label);
        }
    }

    private void onLabelSelect(InteractionXYChart.DataSelectEvent e, List<XYLabel> labels) {

    }

    private @Nullable String validateLabelInput(String line) {
        var kind = labelKind.getValue();
        if (line.isEmpty()) {
            return switch (kind) {
                case atlas -> "text [,ap, dv, ml, color]";
                case reference -> "text [,ap, dv, ml, reference, color]";
                case slice -> "text [,x, y, plane, project, color]";
                case canvas -> "text [,x, y, shank, color]";
                case probe -> "text [,x, y, color]";
            };
        } else {
            try {
                evalLabelInput(kind, line);
                return null;
            } catch (RuntimeException e) {
                return e.getMessage();
            }
        }
    }

    /*==============*
     * label handle *
     *==============*/

    private static final Parameter P_TEXT = new Parameter("text", String.class, "text", "label text");
    private static final Parameter P_AP = new Parameter("ap", double.class, "ap", "0", "AP position in um");
    private static final Parameter P_DV = new Parameter("dv", double.class, "dv", "0", "DV position in um");
    private static final Parameter P_ML = new Parameter("ml", double.class, "ml", "0", "ML position in um");
    private static final Parameter P_REF = new Parameter("reference", String.class, "ref", "''", "reference name");
    private static final Parameter P_PLANE = new Parameter("plane", double.class, "plane", "0", "slice plane position in um");
    private static final Parameter P_X = new Parameter("x", double.class, "x", "0", "");
    private static final Parameter P_Y = new Parameter("y", double.class, "y", "0", "");
    private static final Parameter P_S = new Parameter("shank", int.class, "shank", "0", "based on which shank");
    private static final Parameter P_PROJ = new Parameter("project", String.class, "project", "''", "slice projection");
    private static final Parameter P_COLOR = new Parameter("color", String.class, "color", "'black'", "text color");

    private static final Parameter[] P_ATLAS = new Parameter[]{P_TEXT, P_AP, P_DV, P_ML, P_COLOR};
    private static final Parameter[] P_ATLAS_REF = new Parameter[]{P_TEXT, P_AP, P_DV, P_ML, P_REF, P_COLOR};
    private static final Parameter[] P_SLICE = new Parameter[]{P_TEXT, P_X.withDescription("slice x position in um"), P_Y.withDescription("slice y position in um"), P_PLANE, P_PROJ, P_COLOR};
    private static final Parameter[] P_PROBE = new Parameter[]{P_TEXT, P_X.withDescription("x position in probe (um)"), P_Y.withDescription("y position in probe (um)"), P_S, P_COLOR};
    private static final Parameter[] P_CANVAS = new Parameter[]{P_TEXT, P_X.withDescription("x position in canvas (px)"), P_Y.withDescription("y position in canvas (px)"), P_COLOR};

    private CoordinateLabel evalLabelInput(CoordinateLabel.LabelPositionKind kind, String line) {
        var values = pairScriptArguments(switch (kind) {
            case atlas -> P_ATLAS;
            case reference -> P_ATLAS_REF;
            case slice -> P_SLICE;
            case canvas -> P_PROBE;
            case probe -> P_CANVAS;
        }, parseScriptInputLine(line));

        var text = (String) values[0];
        var color = ((String) values[values.length - 1]).toLowerCase();

        return switch (kind) {
            // text ,ap, dv, ml, color
            case atlas -> new CoordinateLabel(text, new AtlasPosition(new Coordinate((double) values[1], (double) values[2], (double) values[3])), color);
            // text ,ap, dv, ml, reference, color
            case reference -> new CoordinateLabel(text, new AtlasRefPosition((String) values[4], new Coordinate((double) values[1], (double) values[2], (double) values[3])), color);
            // text ,x, y, plane, project, color
            case slice -> {
                var project = (String) values[4];
                ImageSliceStack.Projection projection;
                if (project.isEmpty()) {
                    projection = (atlas == null) ? ImageSliceStack.Projection.coronal : atlas.getProjection();
                } else {
                    projection = ImageSliceStack.Projection.valueOf(project);
                }
                yield new CoordinateLabel(text, new SlicePosition(projection, new SliceCoordinate((double) values[3], (double) values[1], (double) values[2])), color);
            }
            // text ,x, y, shank, color
            case probe -> new CoordinateLabel(text, new ProbePosition(new ProbeCoordinate((int) values[3], (double) values[1], (double) values[2])), color);
            // text ,x, y, color
            case canvas -> new CoordinateLabel(text, new CanvasPosition((double) values[1], (double) values[2]), color);
        };
    }

    public void evalAndAddLabel(CoordinateLabel.LabelPositionKind kind, String line) {
        addLabel(evalLabelInput(kind, line));
    }

    public void addLabel(CoordinateLabel label) {
        var value = colorMapping.computeIfAbsent(label.color(), name -> colormap.addColor(Color.valueOf(name)));
        var xy = new XY(project(label.position()), value, label);
        graphics.addData(xy);
        labels.add(new XYLabel(label, xy));
    }


    private Point2D project(@Nullable LabelPosition pos) {
        //XXX Unsupported Operation AtlasLabelPlugin.project
        throw new UnsupportedOperationException();
    }

    public @Nullable CoordinateLabel getLabel(String text) {
        for (var label : labels) {
            if (label.label.text().equals(text)) {
                return label.label;
            }
        }
        return null;
    }

    private @Nullable XYLabel getLabel(CoordinateLabel label) {
        for (var xyLabel : labels) {
            if (xyLabel.label == label) {
                return xyLabel;
            }
        }
        return null;
    }

    private @Nullable XYLabel getLabel(XY xy) {
        for (var label : labels) {
            if (label.data == xy) {
                return label;
            }
        }
        return null;
    }

    public void removeLabel(CoordinateLabel label) {
        var xy = getLabel(label);
        if (xy != null) removeLabel(xy);
    }

    private void removeLabel(XYLabel label) {
        graphics.removeData(d -> label.data == d);
        labels.remove(label);
    }

    public void clearLabels() {
        graphics.clearData();
        labels.clear();
    }

    public void focusOnLabel(CoordinateLabel label) {
        var xy = getLabel(label);
        if (xy != null) focusOnLabel(xy);
    }

    private void focusOnLabel(XYLabel label) {

    }
}

