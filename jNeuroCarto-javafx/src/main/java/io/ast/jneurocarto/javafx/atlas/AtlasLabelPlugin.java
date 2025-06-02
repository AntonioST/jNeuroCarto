package io.ast.jneurocarto.javafx.atlas;

import java.util.*;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
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
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.PluginStateService;
import io.ast.jneurocarto.javafx.atlas.CoordinateLabel.*;
import io.ast.jneurocarto.javafx.chart.DiscreteColormap;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XYText;
import io.ast.jneurocarto.javafx.script.BlueprintScriptCallable.Parameter;
import io.ast.jneurocarto.javafx.utils.FormattedTextField;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.StateView;

import static io.ast.jneurocarto.javafx.script.BlueprintScriptHandles.pairScriptArguments;
import static io.ast.jneurocarto.javafx.script.BlueprintScriptHandles.parseScriptInputLine;

@NullMarked
public class AtlasLabelPlugin extends InvisibleView implements StateView<AtlasLabelViewState> {

    private @Nullable AtlasPlugin atlas;
    private @Nullable AtlasReferenceState references;
    private InteractionXYPainter foreground;
    private XYText.Builder labelHandle;
    private DiscreteColormap colormap;
    private final Map<String, Integer> colorMapping = new HashMap<>();
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
        for (var label : labels) {
            var color = label.color();
            colorMapping.computeIfAbsent(color, name -> colormap.addColor(Color.valueOf(name)));
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
        labelHandle = foreground.text();
        labelHandle.colormap(colormap = new DiscreteColormap());
        colormap.addColor(0, Color.BLACK);
        colorMapping.put("black", 0);

        var ret = super.setup(service);

        view.addEventFilter(InteractionXYChart.DataTouchEvent.DATA_TOUCH, this::onDataTouch);
        view.addEventFilter(InteractionXYChart.DataSelectEvent.DATA_SELECT, this::onDataSelect);

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

    public void onDataTouch(InteractionXYChart.DataTouchEvent e) {
    }

    public void onDataSelect(InteractionXYChart.DataSelectEvent e) {
    }

    private @Nullable String validateLabelInput(String line) {
        var kind = labelKind.getValue();
        if (line.isEmpty()) {
            return switch (kind) {
                case atlas -> "text [,ap, dv, ml, color]";
                case reference -> "text [,ap, dv, ml, reference, color]";
                case slice -> "text [,x, y, plane, project, color]";
                case canvas, probe -> "text [,x, y, color]";
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
    private static final Parameter P_PROJ = new Parameter("project", String.class, "project", "''", "slice projection");
    private static final Parameter P_COLOR = new Parameter("color", String.class, "color", "'black'", "text color");

    private static final Parameter[] P_ATLAS = new Parameter[]{P_TEXT, P_AP, P_DV, P_ML, P_COLOR};
    private static final Parameter[] P_ATLAS_REF = new Parameter[]{P_TEXT, P_AP, P_DV, P_ML, P_REF, P_COLOR};
    private static final Parameter[] P_SLICE = new Parameter[]{P_TEXT, P_X.withDescription("slice x position in um"), P_Y.withDescription("slice y position in um"), P_PLANE, P_PROJ, P_COLOR};
    private static final Parameter[] P_PROBE = new Parameter[]{P_TEXT, P_X.withDescription("x position in probe (um)"), P_Y.withDescription("y position in probe (um)"), P_COLOR};
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
            // text ,x, y, color
            case canvas -> new CoordinateLabel(text, new CanvasPosition((double) values[1], (double) values[2]), color);
            // text ,x, y, color
            case probe -> new CoordinateLabel(text, new ProbePosition((double) values[1], (double) values[2]), color);
        };
    }

    public void evalAndAddLabel(CoordinateLabel.LabelPositionKind kind, String line) {
        var label = evalLabelInput(kind, line);
        var value = colorMapping.computeIfAbsent(label.color(), name -> colormap.addColor(Color.valueOf(name)));
        labels.add(label);
        labelHandle.addText(label.text(), null, value);
    }
}

