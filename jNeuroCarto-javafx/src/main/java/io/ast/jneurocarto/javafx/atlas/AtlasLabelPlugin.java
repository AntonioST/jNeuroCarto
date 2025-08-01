package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.util.StringConverter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.ImageSliceStack;
import io.ast.jneurocarto.atlas.SliceCoordinate;
import io.ast.jneurocarto.atlas.SliceDomain;
import io.ast.jneurocarto.core.*;
import io.ast.jneurocarto.javafx.app.LogMessageService;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.app.PluginStateService;
import io.ast.jneurocarto.javafx.app.ProbeView;
import io.ast.jneurocarto.javafx.atlas.CoordinateLabel.AtlasPosition;
import io.ast.jneurocarto.javafx.atlas.CoordinateLabel.LabelPosition;
import io.ast.jneurocarto.javafx.atlas.CoordinateLabel.ProbePosition;
import io.ast.jneurocarto.javafx.atlas.CoordinateLabel.SlicePosition;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.data.XY;
import io.ast.jneurocarto.javafx.chart.data.XYText;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseDraggingHandler;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import io.ast.jneurocarto.javafx.chart.event.DataSelectEvent;
import io.ast.jneurocarto.javafx.script.BlueprintScriptCallable.Parameter;
import io.ast.jneurocarto.javafx.utils.DiscreteColorMapping;
import io.ast.jneurocarto.javafx.utils.FormattedTextField;
import io.ast.jneurocarto.javafx.utils.Result;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbeUpdateHandler;
import io.ast.jneurocarto.javafx.view.StateView;

import static io.ast.jneurocarto.javafx.script.BlueprintScriptHandles.pairScriptArguments;
import static io.ast.jneurocarto.javafx.script.BlueprintScriptHandles.parseScriptInputLine;

@NullMarked
public class AtlasLabelPlugin extends InvisibleView implements StateView<AtlasLabelViewState>, ProbeUpdateHandler<Object> {

    private final ProbeDescription<Object> probe;
    private ShankCoordinate shankTransform = ShankCoordinate.ZERO;
    private static final ProbeTransform.Domain<Point2D> CHART_DOMAIN = new ProbeTransform.Project2D("chart");

    private final AtlasPlugin atlas;
    private final AtlasReferenceService references;

    private InteractionXYPainter foreground;
    private XYText graphics;
    private DiscreteColorMapping colormap = new DiscreteColorMapping();

    private final List<XYLabel> labels = new ArrayList<>();

    private final Logger log = LoggerFactory.getLogger(AtlasLabelPlugin.class);

    public AtlasLabelPlugin(ProbeDescription<Object> probe, AtlasPlugin atlas) {
        this.probe = probe;
        this.atlas = atlas;

        references = atlas.getReferencesService();
    }

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

    public final BooleanProperty allowLabelDragging = new SimpleBooleanProperty(false);

    public boolean isAllowLabelDragging() {
        return allowLabelDragging.get();
    }

    public void setAllowLabelDragging(boolean allow) {
        allowLabelDragging.set(allow);
    }

    public final DoubleProperty fontSize = new SimpleDoubleProperty(12);

    public double getFontSize() {
        return fontSize.get();
    }

    public void setFontSize(double size) {
        this.fontSize.set(size);
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

    private void restoreState() {
        var state = PluginStateService.loadGlobalState(AtlasLabelGlobalViewState.class);
        if (state == null) state = new AtlasLabelGlobalViewState();
        restoreState(state);
    }

    private void restoreState(AtlasLabelGlobalViewState state) {
        fontSize.set(state.fontSize);
    }

    /*===========*
     * UI layout *
     *===========*/

    private ChoiceBox<CoordinateLabel.LabelPositionKind> labelKinds;
    private TextField labelText;

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        var layout = super.setup(service);
        restoreState();
        return layout;
    }

    @Override
    protected HBox setupHeading(PluginSetupService service) {
        var layout = super.setupHeading(service);

        var showLabelSwitch = new CheckBox("Show labels");
        showLabelSwitch.selectedProperty().bindBidirectional(showLabels);
        showLabelSwitch.selectedProperty().bindBidirectional(foreground.visible);
        layout.getChildren().add(showLabelSwitch);

        return layout;
    }

    @Override
    protected Node setupContent(PluginSetupService service) {
        labelKinds = new ChoiceBox<>();
        labelKinds.setValue(CoordinateLabel.LabelPositionKind.reference);
        labelKinds.setMaxWidth(120);
        labelKinds.getItems().addAll(Arrays.asList(CoordinateLabel.LabelPositionKind.values()));
        labelKinds.setConverter(new StringConverter<>() {
            @Override
            public String toString(CoordinateLabel.@Nullable LabelPositionKind object) {
                if (object == null) return "";
                if (object == CoordinateLabel.LabelPositionKind.reference) {
                    return "Atlas " + atlas.getAtlasReferenceName() + " coordinate";
                }
                return object.kind;
            }

            @Override
            public CoordinateLabel.LabelPositionKind fromString(String string) {
                throw new UnsupportedOperationException();
            }
        });

        labelText = new TextField();
        var tooltip = FormattedTextField.install(labelText, this::validateLabelInput);
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setHideDelay(Duration.seconds(10));

        var add = new Button("Add");
        add.setOnAction(this::onLabelAdd);

        var remove = new Button("remove");
        remove.setOnAction(this::onLabelRemove);

        var layout = new HBox(labelKinds, labelText, add, remove);
        layout.setSpacing(5);
        HBox.setHgrow(labelText, Priority.ALWAYS);

        return layout;
    }

    @Override
    protected void setupChartContent(PluginSetupService service, ProbeView<?> canvas) {
        foreground = canvas.getForegroundPainter();
        graphics = foreground.text()
            .colormap(colormap.colormap)
            .font(Font.font(15))
            .line(Color.BLACK)
            .showAnchorPoint(true)
            .graphics();
        colormap.put("black", Color.BLACK);

        fontSize.addListener((_, _, size) -> {
            graphics.font(Font.font(size.doubleValue()));
        });

        canvas.addEventFilter(AtlasUpdateEvent.POSITION, _ -> updateLabelPosition());
        canvas.addEventFilter(ChartMouseEvent.CHART_MOUSE_CLICKED, this::onDataTouch);
        canvas.addEventFilter(DataSelectEvent.DATA_SELECT, this::onDataSelect);
        ChartMouseDraggingHandler.setupChartMouseDraggingHandler(canvas, new LabelDraggingHandler());
    }

    /*==============*
     * event handle *
     *==============*/

    private @Nullable List<XYLabel> selected;

    private void onLabelAdd(ActionEvent e) {
        var kind = labelKinds.getValue();
        var text = labelText.getText();
        log.debug("add label[{}] = {}", kind, text);
        evalAndAddLabel(kind, text);
    }

    private void onLabelRemove(ActionEvent e) {
        var kind = labelKinds.getValue();
        var text = labelText.getText();
        log.debug("remove label \"{}\"", text);
        var label = evalLabelInput(kind, text);
        removeLabel(label.text());
    }

    private void onDataTouch(ChartMouseEvent e) {
        var xy = graphics.touch(e.point);
        if (xy != null) {
            var label = findLabel(xy);
            if (label != null) {
                onLabelTouch(e, label);
            }
        }
    }

    private void onDataSelect(DataSelectEvent e) {
        var selected = graphics.touch(e.bounds).stream()
            .map(this::findLabel)
            .filter(Objects::nonNull)
            .toList();

        if (!selected.isEmpty()) {
            onLabelSelect(e, selected);
        }
    }

    private void onLabelTouch(ChartMouseEvent e, XYLabel label) {
        if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
            focusOnLabel(label);
            LogMessageService.printMessage("focus on " + label.text());
            e.consume();
        } else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
            printLabelInformation(label.label);
            e.consume();
        } else if (e.getButton() == MouseButton.MIDDLE && e.getClickCount() == 1) {
            var text = label.text();
            labelText.setText(text);
            labelText.positionCaret(text.length());
            e.consume();
        }
    }

    private void onLabelSelect(DataSelectEvent e, List<XYLabel> labels) {
        if (isAllowLabelDragging()) {
            selected = labels;
        }
        log.debug("selected {} labels", labels.size());
    }

    private class LabelDraggingHandler implements ChartMouseDraggingHandler {
        @Override
        public boolean onChartMouseDragDetect(ChartMouseEvent e) {
            if (!isAllowLabelDragging()) return false;

            if (selected == null) {
                var xy = graphics.touch(e.point);
                if (xy != null) {
                    var label = findLabel(xy);
                    if (label != null) {
                        selected = List.of(label);
                    }
                }
            }
            return selected != null;
        }

        @Override
        public void onChartMouseDragging(ChartMouseEvent p, ChartMouseEvent e) {
            if (selected == null) return;

            var transform = new LabelPositionTransformer();
            var offset = e.delta(p);
            for (var label : selected) {
                var pos = transform.offset(label.position(), offset);
                if (pos != null) {
                    var x = label.data.x() + offset.getX();
                    var y = label.data.y() + offset.getY();
                    label.data.x(x);
                    label.data.y(y);
                    label.updatePosition(pos);
                }
            }

            foreground.repaint();
        }

        @Override
        public void onChartMouseDragDone(ChartMouseEvent e) {
            if (selected == null) return;

            selected = null;
        }
    }

    private @Nullable Result<String, Throwable> validateLabelInput(String line) {
        var kind = labelKinds.getValue();
        if (line.isEmpty()) {
            return Result.success(getInputSignature(kind));
        } else {
            try {
                evalLabelInput(kind, line);
                return null;
            } catch (RuntimeException e) {
                return Result.fail(new RuntimeException(e.getMessage() + "; " + getInputSignature(kind), e));
            }
        }
    }

    private String getInputSignature(CoordinateLabel.LabelPositionKind kind) {
        return switch (kind) {
            case atlas -> "text [,ap, dv, ml, color]";
            case reference -> "text [,ap, dv, ml, reference, color]";
            case slice -> "text [,x, y, plane, project, color]";
            case probe -> "text [,x, y, shank, color]";
        };
    }

    /*=============*
     * probe event *
     *=============*/

    @Override
    public void onProbeUpdate(Object chmap, List<ElectrodeDescription> blueprint) {
        shankTransform = probe.getShankCoordinate(chmap);
    }

    /*==============*
     * label handle *
     *==============*/

    private static final Parameter P_TEXT = new Parameter("text", String.class, "text", "label text");
    private static final Parameter P_AP = new Parameter("ap", double.class, "ap", "0", "AP position in mm");
    private static final Parameter P_DV = new Parameter("dv", double.class, "dv", "0", "DV position in mm");
    private static final Parameter P_ML = new Parameter("ml", double.class, "ml", "0", "ML position in mm");
    private static final Parameter P_REF = new Parameter("reference", String.class, "ref", "''", "reference name");
    private static final Parameter P_PLANE = new Parameter("plane", double.class, "plane", "0", "slice plane position in mm");
    private static final Parameter P_X = new Parameter("x", double.class, "x", "0", "");
    private static final Parameter P_Y = new Parameter("y", double.class, "y", "0", "");
    private static final Parameter P_S = new Parameter("shank", int.class, "shank", "0", "based on which shank");
    private static final Parameter P_PROJ = new Parameter("project", ImageSliceStack.Projection.class, "project", "''", "slice projection");
    private static final Parameter P_COLOR = new Parameter("color", String.class, "color", "'black'", "text color");

    private static final Parameter[] P_ATLAS = new Parameter[]{P_TEXT, P_AP, P_DV, P_ML, P_COLOR};
    private static final Parameter[] P_ATLAS_REF = new Parameter[]{P_TEXT, P_AP, P_DV, P_ML, P_REF, P_COLOR};
    private static final Parameter[] P_SLICE = new Parameter[]{P_TEXT, P_X.withDescription("slice x position in mm"), P_Y.withDescription("slice y position in mm"), P_PLANE, P_PROJ, P_COLOR};
    private static final Parameter[] P_PROBE = new Parameter[]{P_TEXT, P_X.withDescription("x position in probe (mm)"), P_Y.withDescription("y position in probe (mm)"), P_S, P_COLOR};

    private CoordinateLabel evalLabelInput(CoordinateLabel.LabelPositionKind kind, String line) {
        var values = pairScriptArguments(switch (kind) {
            case atlas -> P_ATLAS;
            case reference -> P_ATLAS_REF;
            case slice -> P_SLICE;
            case probe -> P_PROBE;
        }, parseScriptInputLine(line));

        var text = (String) values[0];
        var color = ((String) values[values.length - 1]).toLowerCase();

        LabelPosition pos = switch (kind) {
            // text ,ap, dv, ml, color
            case atlas -> {
                var ap = (double) values[1] * 1000;
                var dv = (double) values[2] * 1000;
                var ml = (double) values[3] * 1000;
                yield new AtlasPosition(new Coordinate(ap, dv, ml), null);
            }
            // text ,ap, dv, ml, reference, color
            case reference -> {
                var ref = (String) values[4];
                if (ref.isEmpty()) ref = atlas.getAtlasReferenceName();
                var ap = (double) values[1] * 1000;
                var dv = (double) values[2] * 1000;
                var ml = (double) values[3] * 1000;
                yield new AtlasPosition(new Coordinate(ap, dv, ml), ref);
            }
            // text ,x, y, plane, project, color
            case slice -> {
                var project = (ImageSliceStack.Projection) values[4];
                if (project == null) {
                    project = atlas.getProjection();
                }
                var x = (double) values[1] * 1000;
                var y = (double) values[2] * 1000;
                var p = (double) values[3] * 1000;
                yield new SlicePosition(project, new SliceCoordinate(p, x, y));
            }
            // text ,x, y, shank, color
            case probe -> {
                var x = (double) values[1] * 1000;
                var y = (double) values[2] * 1000;
                var s = (int) values[3];
                yield new ProbePosition(new ProbeCoordinate(s, x, y));
            }
        };

        return new CoordinateLabel(text, pos, color);
    }

    public void evalAndAddLabel(CoordinateLabel.LabelPositionKind kind, String line) {
        var label = evalLabelInput(kind, line);
        var found = findLabel(label.text());
        if (found == null) {
            addLabel(label);
        } else {
            log.debug("update {}", label);
            found.label = label;
            updateLabelPosition(found);
        }
    }

    public void addLabel(CoordinateLabel label) {
        log.debug("add {}", label);

        var value = colormap.applyAsDouble(label.color());
        log.debug("use color {} = {}", label.color(), value);
        var xy = new XY(0, 0, value, label.text());
        graphics.addData(xy);

        var ret = new XYLabel(label, xy);
        labels.add(ret);
        updateLabelPosition(ret);
    }

    public @Nullable CoordinateLabel getLabel(String text) {
        for (var label : labels) {
            if (label.label.text().equals(text)) {
                return label.label;
            }
        }
        return null;
    }

    private @Nullable XYLabel findLabel(String text) {
        for (var label : labels) {
            if (label.text().equals(text)) {
                return label;
            }
        }
        return null;
    }

    private @Nullable XYLabel findLabel(CoordinateLabel label) {
        for (var xyLabel : labels) {
            if (xyLabel.label == label) {
                return xyLabel;
            }
        }
        return null;
    }

    private @Nullable XYLabel findLabel(XY xy) {
        for (var label : labels) {
            if (label.data == xy) {
                return label;
            }
        }
        return null;
    }

    public boolean isVisible(String label) {
        var xy = findLabel(label);
        return xy != null && xy.visible;
    }

    public boolean isVisible(CoordinateLabel label) {
        var xy = findLabel(label);
        return xy != null && xy.visible;
    }

    public void setVisible(String label, boolean visible) {
        var xy = findLabel(label);
        if (xy != null) xy.visible = visible;
    }

    public void setVisible(CoordinateLabel label, boolean visible) {
        var xy = findLabel(label);
        if (xy != null) xy.visible = visible;
    }

    public void removeLabel(String label) {
        log.debug("remove label \"{}\"", label);
        var xy = findLabel(label);
        if (xy != null) removeLabel(xy);
    }

    public void removeLabel(CoordinateLabel label) {
        log.debug("remove label \"{}\"", label.text());
        var xy = findLabel(label);
        if (xy != null) removeLabel(xy);
    }

    private void removeLabel(XYLabel label) {
        graphics.removeData(d -> label.data == d);
        labels.remove(label);
        foreground.repaint();
    }

    public void clearLabels() {
        log.debug("clear labels");
        graphics.clearData();
        labels.clear();
        foreground.repaint();
    }

    public void focusOnLabel(CoordinateLabel label) {
        var xy = findLabel(label);
        if (xy != null) focusOnLabel(xy);
    }

    private void focusOnLabel(XYLabel label) {
        log.debug("focus label = {}", label.text());
        var pos = new LabelPositionTransformer().projectToAnatomical(label.position());
        if (pos != null) atlas.anchorImageTo(atlas.project(pos));
    }

    public void printLabelInformation(CoordinateLabel label) {
        var transform = new LabelPositionTransformer();

        var messages = new ArrayList<String>();
        messages.add(label.text());

        var coor = transform.projectToAnatomical(label.position());
        if (coor != null) {
            coor = atlas.project(coor);
            String ref = "";
            if (transform.reference != null) {
                ref = " to " + transform.reference;
            }

            messages.add("(AP,DV,ML)=(%.1f,%.1f,%.1f)mm%s".formatted(coor.ap() / 1000, coor.dv() / 1000, coor.ml() / 1000, ref));
        }

        var p = projectToChart(label.position());
        if (p != null) {
            messages.add("(X,Y)=(%.0f,%.0f)um".formatted(p.getX(), p.getY()));
        }

        LogMessageService.printMessage(messages);
    }

    /*===============================*
     * label position transformation *
     *===============================*/

    private static final class XYLabel {
        private CoordinateLabel label;
        private final XY data;
        boolean visible;

        private XYLabel(CoordinateLabel label, XY data) {
            this.label = label;
            this.data = data;
        }

        public CoordinateLabel label() {
            return label;
        }

        public String text() {
            return label.text();
        }

        public LabelPosition position() {
            return label.position();
        }

        public void updatePosition(LabelPosition position) {
            this.label = label.withPosition(position);
        }

        public void updateLabel(CoordinateLabel label) {
            this.label = label;
            this.data.external(label.text());
        }

        public void setChartPosition(@Nullable Point2D p) {
            if (p == null || !visible) {
                data.x(Double.NaN);
            } else {
                data.x(p.getX());
                data.y(p.getY());
            }
        }
    }

    private @Nullable Point2D projectToChart(LabelPosition pos) {
        return new LabelPositionTransformer().projectToChart(pos);
    }

    private @Nullable Coordinate projectToAnatomical(LabelPosition pos) {
        return new LabelPositionTransformer().projectToAnatomical(pos);
    }

    private void updateLabelPosition() {
        var transform = new LabelPositionTransformer();
        for (var label : labels) {
            var pos = transform.projectToChart(label.position());
            log.trace("update label \"{}\" to {}", label.text(), pos);
            label.setChartPosition(pos);
        }
        foreground.repaint();
    }

    private void updateLabelPosition(XYLabel label) {
        var transform = new LabelPositionTransformer();
        var pos = transform.projectToChart(label.position());
        log.debug("update label \"{}\" to {}", label.text(), pos);
        label.setChartPosition(pos);
        foreground.repaint();
    }

    public void changeLabelPosition(XYLabel label, Point2D offset) {
        var transform = new LabelPositionTransformer();
        var pos = transform.offset(label.position(), offset);
        if (pos == null) return;

        var x = label.data.x() + offset.getX();
        var y = label.data.y() + offset.getY();
        label.data.x(x);
        label.data.y(y);
        label.updatePosition(pos);
    }

    private class LabelPositionTransformer {
        private final @Nullable String reference;
        private final ImageSliceStack.Projection projection;

        /**
         * slice coordinate to chart coordinate
         */
        private @Nullable ProbeTransform<SliceCoordinate, Point2D> cs;

        /**
         * chart coordinate to slice coordinate
         */
        private @Nullable ProbeTransform<Point2D, SliceCoordinate> sc;

        /**
         * probe coordinate to slice coordinate
         */
        private @Nullable ProbeTransform<ProbeCoordinate, SliceCoordinate> sp;

        /**
         * global anatomical coordinate to slice coordinate
         */
        private @Nullable ProbeTransform<Coordinate, SliceCoordinate> sg;

        /**
         * slice coordinate to global anatomical coordinate
         */
        private @Nullable ProbeTransform<SliceCoordinate, Coordinate> gs;

        /**
         * zero-z slice coordinate to slice coordinate
         */
        private @Nullable ProbeTransform<SliceCoordinate, SliceCoordinate> ss;

        LabelPositionTransformer() {
            reference = atlas.getAtlasReferenceName();
            projection = atlas.getProjection();
        }

        private Coordinate gr(Coordinate c) {
            return atlas.pullback(c);
        }

        private @Nullable Coordinate gr(String reference, Coordinate c) {
            var ref = references.getReference(reference);
            if (ref == null) return null;
            return ref.getTransform().inverseTransform(c);
        }

        private @Nullable Point2D cs(@Nullable SliceCoordinate c) {
            if (c == null) return null;
            if (cs == null) {
                cs = ProbeTransform.create(SliceDomain.INSTANCE, CHART_DOMAIN, atlas.painter().getChartTransform());
            }
            return cs.transform(c);
        }

        private Point3D scd(Point2D c) {
            if (sc == null) {
                sc = ProbeTransform.create(CHART_DOMAIN, SliceDomain.INSTANCE, atlas.painter().getImageTransform());
            }
            return sc.deltaTransform(new Point3D(c.getX(), c.getY(), 0));
        }

        private @Nullable SliceCoordinate sp(@Nullable ProbeCoordinate c) {
            if (c == null) return null;
            if (sp == null) {
                sp = ProbeTransform.create(ProbeTransform.PROBE, SliceDomain.INSTANCE, atlas.painter().getImageTransform());
            }
            return sp.transform(c);
        }

        private @Nullable SliceCoordinate sg(@Nullable Coordinate c) {
            if (c == null) return null;
            if (sg == null) {
                var image = atlas.getImageSliceStack();
                if (image == null) return null;
                sg = image.getSliceTransform();
            }
            return sg.transform(c);
        }

        private @Nullable Coordinate gs(@Nullable SliceCoordinate c) {
            if (c == null) return null;
            if (gs == null) {
                var image = atlas.getImageSliceStack();
                if (image == null) return null;
                gs = image.getTransform();
            }
            return gs.transform(c);
        }

        private @Nullable Point3D gsd(@Nullable Point3D c) {
            if (c == null) return null;
            if (gs == null) {
                var image = atlas.getImageSliceStack();
                if (image == null) return null;
                gs = image.getTransform();
            }
            return gs.deltaTransform(c);
        }

        private @Nullable SliceCoordinate ss(@Nullable SliceCoordinate c) {
            if (c == null) return null;
            if (ss == null) {
                var image = atlas.getImageSlice();
                if (image == null) return null;
                ss = image.getPlaneAtTransform();
            }
            return ss.transform(c);
        }

        @Nullable
        Point2D projectToChart(LabelPosition pos) {
            return switch (pos) {
                case AtlasPosition(var coor, var ref) when ref == null -> cs(sg(coor));
                case AtlasPosition(var coor, var ref) when ref.equals(reference) -> cs(sg(gr(coor)));
                case AtlasPosition(var coor, var ref) -> cs(sg(gr(ref, coor)));
                case SlicePosition(var proj, var _) when proj != projection -> null;
                case SlicePosition(var _, var coor) -> cs(coor);
                case ProbePosition(var coor) -> {
                    var c = shankTransform.toShank(coor, 0);
                    yield new Point2D(c.x(), c.y());
                }
            };
        }

        @Nullable
        Coordinate projectToAnatomical(LabelPosition pos) {
            return switch (pos) {
                case AtlasPosition(var coor, var ref) when ref == null -> coor;
                case AtlasPosition(var coor, var ref) when ref.equals(reference) -> gr(coor);
                case AtlasPosition(var coor, var ref) -> gr(ref, coor);
                case SlicePosition(var proj, var _) when proj != projection -> null;
                case SlicePosition(var _, var coor) -> gs(coor);
                case ProbePosition(var coor) -> gs(ss(sp(shankTransform.toShank(coor, 0))));
            };
        }

        @Nullable
        LabelPosition offset(LabelPosition pos, Point2D offset) {
            return switch (pos) {
                case AtlasPosition(Coordinate(var ap, var dv, var ml), var ref) -> {
                    var o = gsd(scd(offset)); // TODO AP flipped?
                    yield new AtlasPosition(new Coordinate(ap + o.getX(), dv + o.getY(), ml + o.getZ()), ref);
                }
                case SlicePosition(var proj, var _) when proj != projection -> null;
                case SlicePosition(var proj, SliceCoordinate(var p, var x, var y)) -> {
                    var o = scd(offset);
                    yield new SlicePosition(proj, new SliceCoordinate(p + o.getZ(), x + o.getX(), y + o.getY()));
                }
                case ProbePosition(ProbeCoordinate(var s, var x, var y, var z)) -> {
                    yield new ProbePosition(new ProbeCoordinate(s, x + offset.getX(), y + offset.getY(), z));
                }
            };
        }
    }
}

