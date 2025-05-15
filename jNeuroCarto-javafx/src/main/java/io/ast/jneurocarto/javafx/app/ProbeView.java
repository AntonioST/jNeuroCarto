package io.ast.jneurocarto.javafx.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

@NullMarked
public class ProbeView<T> extends StackPane {

    private final CartoConfig config;
    private final ProbeDescription<T> probe;
    private final Map<String, ScatterChart.Series<Number, Number>> electrodes = new HashMap<>();
    private final Map<Integer, String> allStateMap;
    private static final String STATE_HIGHLIGHTED = "_highlighted_";

    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final ScatterChart<Number, Number> scatter;
    private final Canvas background;
    private final Canvas foreground;

    private @Nullable T channelmap;
    private @Nullable List<ElectrodeDescription> blueprint;

    public ProbeView(CartoConfig config, ProbeDescription<T> probe) {
        this.config = config;
        this.probe = probe;

        xAxis = new NumberAxis("(um)", 0, 1000, 100);
        yAxis = new NumberAxis("(um)", 0, 1000, 100);
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);

        scatter = new ScatterChart<>(xAxis, yAxis);
        scatter.setLegendVisible(false);

        background = new Canvas();
        background.widthProperty().bind(scatter.widthProperty());
        background.heightProperty().bind(scatter.heightProperty());

        foreground = new Canvas();
        foreground.setMouseTransparent(false);
        foreground.widthProperty().bind(scatter.widthProperty());
        foreground.heightProperty().bind(scatter.heightProperty());

        getChildren().addAll(background, scatter, foreground);

        foreground.setOnMousePressed(this::onMousePressed);
        foreground.setOnMouseReleased(this::onMouseReleased);
        foreground.setOnMouseDragged(this::onMouseDragged);
        foreground.setOnScroll(this::onMouseWheeled);


        allStateMap = probe.allStates();

        var series = allStateMap.values().stream()
          .map(this::newElectrodeSeries)
          .toList();
        scatter.getData().addAll(series);

        scatter.getData().add(newElectrodeSeries(STATE_HIGHLIGHTED));
    }

    public static ScatterChart.Data<Number, Number> asData(ElectrodeDescription e) {
        return new ScatterChart.Data<>(e.x(), e.y(), e);
    }

//    public static Series<Number, Number> asSeries(String name, List<ElectrodeDescription> e) {
//        var ret = new Series<Number, Number>();
//        ret.setName(name);
//        ret.getData().addAll(e.stream().map(ProbeView::asData).toList());
//        return ret;
//    }

    private ScatterChart.Series<Number, Number> newElectrodeSeries(String name) {
        var series = new ScatterChart.Series<Number, Number>();
        series.setName(name);
        electrodes.put(name, series);
        return series;
    }

    /**
     * replace corresponding series by the electrodes at certain state.
     *
     * @param code state code
     * @param e
     * @return updated series.
     */
    private ScatterChart.Series<Number, Number> setSeries(int code, List<ElectrodeDescription> e) {
        var series = electrodes.get(probe.stateOf(code));
        if (series == null) throw new IllegalArgumentException();
        var c = e.stream().filter(it -> it.state() == code).toList();
        return setSeries(series, c);
    }

    /**
     * replace corresponding series by the electrodes.
     *
     * @param name series name.
     * @param e    electrodes.
     * @return updated series.
     */
    private ScatterChart.Series<Number, Number> setSeries(String name, List<ElectrodeDescription> e) {
        var series = electrodes.get(name);
        if (series == null) throw new IllegalArgumentException();
        return setSeries(series, e);
    }


    private ScatterChart.Series<Number, Number> setSeries(ScatterChart.Series<Number, Number> series, List<ElectrodeDescription> e) {
        series.getData().clear();
        if (!e.isEmpty()) {
            addSeries(series, e);
        }
        return series;
    }

    private ScatterChart.Series<Number, Number> addSeries(ScatterChart.Series<Number, Number> series, List<ElectrodeDescription> e) {
        series.getData().addAll(e.stream().map(ProbeView::asData).toList());
        var style = getCssStyleByState(series.getName());
        if (style != null) {
            applyStyle(series, style);
        }
        return series;
    }

    private @Nullable String getCssStyleByState(String state) {
        if (STATE_HIGHLIGHTED.equals(state)) {
            return """
              -fx-background-color: rgba(255,255,0,0.5);
              -fx-background-radius: 6px;
              """;
        }

        var code = probe.stateOf(state);
        if (code.isEmpty()) return null;

        return switch (code.getAsInt()) {
            case ProbeDescription.STATE_UNUSED -> "-fx-background-color: black;";
            case ProbeDescription.STATE_USED -> "-fx-background-color: green;";
            case ProbeDescription.STATE_DISABLED -> """
              -fx-background-color: rgba(255,0,0,0.2);
              -fx-background-radius: 2px;
              """;
            default -> null;
        };
    }

    public static void applyStyle(ScatterChart.Series<Number, Number> series, String css) {
        for (var data : series.getData()) {
            var node = data.getNode();
            if (node != null) {
                node.setStyle(css);
            }
        }
    }

    /*============*
     * channelmap *
     *============*/

    public @Nullable T getChannelmap() {
        return channelmap;
    }

    /**
     * @return new channelmap, {@code null} if failed.
     */
    public @Nullable T resetChannelmap() {
        var channelmap = this.channelmap;
        if (channelmap == null) {
            blueprint = null;
            return null;
        }

        var code = probe.channelmapCode(channelmap);
        if (code == null) return null;

        return resetChannelmap(code);
    }

    public T resetChannelmap(String code) {
        var channelmap = probe.newChannelmap(code);
        setChannelmap(channelmap);
        return channelmap;
    }

    public void setChannelmap(T channelmap) {
        this.channelmap = channelmap;
        setBlueprint(probe.allElectrodes(channelmap));
    }

    /*===========*
     * blueprint *
     *===========*/

    public @Nullable List<ElectrodeDescription> getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(List<ElectrodeDescription> blueprint) {
        this.blueprint = blueprint;
        resetElectrodeState();
    }

    /**
     * reset blueprint, and updating {@link ElectrodeDescription#state()} by {@link #channelmap}'s channels.
     */
    private void resetElectrodeState() {
        var blueprint = this.blueprint;
        if (blueprint == null) return;

        for (var e : blueprint) {
            e.state(ProbeDescription.STATE_UNUSED);
        }

        var channelmap = this.channelmap;
        if (channelmap == null) return;

        var channels = probe.allChannels(channelmap, blueprint);
        for (var e : probe.getInvalidElectrodes(channelmap, channels, blueprint)) {
            e.state(ProbeDescription.STATE_DISABLED);
        }
        for (var e : channels) {
            e.state(ProbeDescription.STATE_USED);
        }
    }

    private void updateElectrode() {
        var blueprint = this.blueprint;
        if (blueprint == null) return;

        for (var state : allStateMap.keySet()) {
            setSeries(state, blueprint);
        }

        setSeries(STATE_HIGHLIGHTED, List.of());
    }

    /*=====================*
     * Mouse event handler *
     *=====================*/

    private @Nullable MouseEvent mousePress;
    private @Nullable MouseEvent mouseMoving;
    private @Nullable NumberAxis previousXAxis;
    private @Nullable NumberAxis previousYAxis;
    private @Nullable Bounds previousArea;

    private void onMousePressed(MouseEvent e) {
        mousePress = e;
        if (e.getButton() == MouseButton.SECONDARY) {
            previousXAxis = new NumberAxis(xAxis.getLowerBound(), xAxis.getUpperBound(), 1);
            previousYAxis = new NumberAxis(yAxis.getLowerBound(), yAxis.getUpperBound(), 1);
            previousArea = getPlottingArea();
        }
    }

    private void onMouseDragged(MouseEvent e) {
        mouseMoving = e;

        var start = mousePress;
        if (start != null) {
            switch (start.getButton()) {
            case MouseButton.PRIMARY -> onMouseSelecting(start, e);
            case MouseButton.SECONDARY -> onMouseDragging(start, e);
            }
        }
    }

    private void onMouseReleased(MouseEvent e) {
        var start = mousePress;
        mousePress = null;
        mouseMoving = null;
        previousXAxis = null;
        previousYAxis = null;
        previousArea = null;

        if (start != null) {
            switch (start.getButton()) {
            case MouseButton.PRIMARY -> onMouseSelected(start, e);
            case MouseButton.SECONDARY -> onMouseDragged(start, e);
            }
        }

        var gc = foreground.getGraphicsContext2D();
        var w = foreground.getWidth();
        var h = foreground.getHeight();
        gc.clearRect(0, 0, w, h);
    }

    public void onMouseWheeled(ScrollEvent e) {
        var delta = e.getDeltaY();
        if (Math.abs(delta) < 1) return;

        var scale = Math.signum(delta) * 0.02;
        var px = e.getX();
        var py = e.getY();
        var p = new Point2D(px, py);

        boolean scaleX = false;
        boolean scaleY = false;

        if (getPlottingArea().contains(p)) {
            scaleY = scaleX = true;
        } else if (getXAxisArea().contains(p)) {
            scaleX = true;
        } else if (getYAxisArea().contains(p)) {
            scaleY = true;
        }

        if (scaleX) {
            var x1 = xAxis.getLowerBound();
            var x2 = xAxis.getUpperBound();
            var r1 = (px) / foreground.getWidth();
            var d1 = (x2 - x1) * scale * r1;
            var d2 = (x2 - x1) * scale * (1 - r1);
            setAxisBoundary(xAxis, x1 - d1, x2 + d2);
        }

        if (scaleY) {
            var x1 = yAxis.getLowerBound();
            var x2 = yAxis.getUpperBound();
            var r1 = (py) / foreground.getHeight();
            var d1 = (x2 - x1) * scale * (1 - r1);
            var d2 = (x2 - x1) * scale * r1;
            setAxisBoundary(yAxis, x1 - d1, x2 + d2);
        }

    }

    private void onMouseSelecting(MouseEvent start, MouseEvent current) {
        var area = getPlottingArea();
        var x1 = Math.max(Math.min(start.getX(), current.getX()), area.getMinX());
        var x2 = Math.min(Math.max(start.getX(), current.getX()), area.getMaxX());
        var y1 = Math.max(Math.min(start.getY(), current.getY()), area.getMinY());
        var y2 = Math.min(Math.max(start.getY(), current.getY()), area.getMaxY());
        var w = x2 - x1;
        var h = y2 - y1;

        var gc = foreground.getGraphicsContext2D();
        gc.clearRect(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());

        gc.setStroke(Color.BLUE);
        gc.setGlobalAlpha(0.3);
        gc.fillRect(x1, y1, w, h);

        gc.setFill(Color.CYAN);
        gc.setGlobalAlpha(0.1);
        gc.strokeRect(x1, y1, w, h);
    }

    private void onMouseSelected(MouseEvent start, MouseEvent end) {
    }

    private void onMouseDragging(MouseEvent start, MouseEvent current) {
        var area = Objects.requireNonNull(previousArea);
        var dx = current.getX() - start.getX();
        var dy = current.getY() - start.getY();

        var ax = Objects.requireNonNull(previousXAxis);
        var x1 = ax.getLowerBound();
        var x2 = ax.getUpperBound();
        dx = dx * (x2 - x1) / area.getWidth();
        setAxisBoundary(xAxis, x1 - dx, x2 - dx);

        ax = Objects.requireNonNull(previousYAxis);
        x1 = ax.getLowerBound();
        x2 = ax.getUpperBound();
        dy = -dy * (x2 - x1) / area.getHeight();
        setAxisBoundary(yAxis, x1 - dy, x2 - dy);
    }

    private void onMouseDragged(MouseEvent start, MouseEvent end) {
    }

    /*==============================*
     * plotting area transformation *
     *==============================*/

    private Bounds getXAxisArea() {
        return foreground.sceneToLocal(xAxis.localToScene(xAxis.getBoundsInLocal()));
    }

    private Bounds getYAxisArea() {
        return foreground.sceneToLocal(yAxis.localToScene(yAxis.getBoundsInLocal()));
    }

    private Bounds getPlottingArea() {
        // https://openjfx.io/javadoc/24/javafx.graphics/javafx/scene/doc-files/cssref.html#xychart
        var plot = scatter.lookup(".chart-plot-background");
        return foreground.sceneToLocal(plot.localToScene(plot.getBoundsInLocal()));
    }

    private GraphicsContext getForebroundCanvasGraphicsContext() {
        var gc = foreground.getGraphicsContext2D();
        gc.setTransform(getCanvasTransform());
        return gc;
    }

    private GraphicsContext getBackbroundCanvasGraphicsContext() {
        var gc = foreground.getGraphicsContext2D();
        gc.setTransform(getCanvasTransform());
        return gc;
    }

    private Affine getCanvasTransform() {
        var ax = xAxis;
        var ay = yAxis;
        var area = getPlottingArea();
        var w = ax.getUpperBound() - ax.getLowerBound();
        var h = ay.getUpperBound() - ay.getLowerBound();

        var mxx = area.getWidth() / w;
        var mxy = 0;
        var mxt = area.getMinX() - ax.getLowerBound() * area.getWidth() / w;
        var myx = 0;
        var myy = -area.getHeight() / h;
        var myt = area.getMaxY() + ay.getLowerBound() * area.getHeight() / h;
        return new Affine(mxx, mxy, mxt, myx, myy, myt);
    }

    private Affine getScatterTransform() {
        var ax = xAxis;
        var ay = yAxis;
        var area = getPlottingArea();
        var w = ax.getUpperBound() - ax.getLowerBound();
        var h = ay.getUpperBound() - ay.getLowerBound();

        var mxx = w / area.getWidth();
        var mxy = 0;
        var mxt = ax.getLowerBound() - area.getMinX() * w / area.getWidth();
        var myx = 0;
        var myy = -h / area.getHeight();
        var myt = ay.getLowerBound() + area.getMaxY() * h / area.getHeight();
        return new Affine(mxx, mxy, mxt, myx, myy, myt);
    }

    public void resetAxesBoundaries() {
        setAxesBoundaries(0, 1000, 0, 1000);
    }

    public void setAxesBoundaries(double x1, double x2, double y1, double y2) {
        setAxisBoundary(xAxis, x1, x2);
        setAxisBoundary(yAxis, y1, y2);
    }

    private static void setAxisBoundary(NumberAxis axis, double x1, double x2) {
        axis.setLowerBound(x1);
        axis.setUpperBound(x2);

        var px = Math.log10(x2 - x1);
        if (px - (int) px < 1e-3) --px;


        var ux = Math.pow(10, (int) px);
        if ((x2 - x1) / ux < 4) ux = Math.pow(10, (int) px - 1);
        axis.setTickUnit(ux);
    }
}
