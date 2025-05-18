package io.ast.jneurocarto.javafx.app;

import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.javafx.utils.StylesheetsUtils;

@NullMarked
public class InteractionXYChart<C extends XYChart<Number, Number>> extends StackPane {

    private final C chart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final Canvas background;
    private final Canvas foreground;
    private final Logger log = LoggerFactory.getLogger(InteractionXYChart.class);

    private double resetX1;
    private double resetX2;
    private double resetY1;
    private double resetY2;

    public InteractionXYChart(C chart) {
        this.chart = chart;
        // https://openjfx.io/javadoc/24/javafx.graphics/javafx/scene/doc-files/cssref.html#xychart
        var plot = chart.lookup(".chart-plot-background");
        plot.setStyle("-fx-background-color: transparent;");

        xAxis = (NumberAxis) chart.getXAxis();
        yAxis = (NumberAxis) chart.getYAxis();
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        resetX1 = xAxis.getLowerBound();
        resetX2 = xAxis.getUpperBound();
        resetY1 = yAxis.getLowerBound();
        resetY2 = yAxis.getUpperBound();

        background = new Canvas();
        background.widthProperty().bind(chart.widthProperty());
        background.heightProperty().bind(chart.heightProperty());

        foreground = new Canvas();
        foreground.setMouseTransparent(false);
        foreground.widthProperty().bind(chart.widthProperty());
        foreground.heightProperty().bind(chart.heightProperty());

        getChildren().addAll(background, chart, foreground);

        foreground.setOnMousePressed(this::onMousePressed);
        foreground.setOnMouseReleased(this::onMouseReleased);
        foreground.setOnMouseDragged(this::onMouseDragged);
        foreground.setOnMouseClicked(this::onMouseClicked);
        foreground.setOnScroll(this::onMouseWheeled);
    }

    public C getChart() {
        return chart;
    }

    public static void applyStyleClass(XYChart.Data<Number, Number> data, String css) {
        var node = data.getNode();
        if (node != null) {
            StylesheetsUtils.addStyleClass(node, css);
        }
    }

    public static void applyStyleClass(XYChart.Series<Number, Number> series, String css) {
        for (var data : series.getData()) {
            applyStyleClass(data, css);
        }
    }

    public static void removeStyleClass(XYChart.Data<Number, Number> data, String css) {
        var node = data.getNode();
        if (node != null) {
            StylesheetsUtils.removeStyleClass(node, css);
        }
    }

    public static void removeStyleClass(XYChart.Series<Number, Number> series, String css) {
        for (var data : series.getData()) {
            removeStyleClass(data, css);
        }
    }

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
            case MouseButton.SECONDARY -> {
                if (start.isControlDown()) {
                    onMouseSelecting(start, e);
                } else {
                    onMouseDragging(start, e);
                }
            }
            }
        }
    }

    private void onMouseReleased(MouseEvent e) {
        var start = mousePress;
        var moving = mouseMoving;

        mousePress = null;
        mouseMoving = null;
        previousXAxis = null;
        previousYAxis = null;
        previousArea = null;

        if (start != null) {
            if (start.getButton() == MouseButton.PRIMARY && moving != null) {
                onMouseSelected(start, e);
            } else if (start.getButton() == MouseButton.SECONDARY && start.isControlDown()) {
                onMouseSelectZooming(start, e);
            }
        }

        var gc = foreground.getGraphicsContext2D();
        var w = foreground.getWidth();
        var h = foreground.getHeight();
        gc.clearRect(0, 0, w, h);
    }

    private void onMouseClicked(MouseEvent e) {
        fireDataTouchEvent(new Point2D(e.getX(), e.getY()), e.getButton());
    }

    private void onMouseWheeled(ScrollEvent e) {
        var delta = e.getDeltaY();
        if (Math.abs(delta) < 1) return;

        var scale = -Math.signum(delta) * 0.02;
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

        if (scaleX || scaleY) fireCanvasChange(CanvasChangeEvent.SCALING);
    }

    private void onMouseSelecting(MouseEvent start, MouseEvent current) {
        var gc = foreground.getGraphicsContext2D();
        var area = getPlottingArea();
        gc.clearRect(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());

        var rect = getMouseSelectBound(start, current);

        gc.setStroke(Color.BLUE);
        gc.setGlobalAlpha(0.3);
        gc.fillRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight());

        gc.setFill(Color.CYAN);
        gc.setGlobalAlpha(0.1);
        gc.strokeRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight());
    }

    private void onMouseSelected(MouseEvent start, MouseEvent end) {
        var bound = getMouseSelectBound(start, end);
        log.trace("onMouseSelected {}", bound);
        fireDataSelectEvent(bound);
    }

    private void onMouseSelectZooming(MouseEvent start, MouseEvent end) {
        var transform = getChartTransform();
        var bound = getMouseSelectBound(start, end);
        bound = transform.transform(bound);
        log.trace("onMouseSelectZooming {} (transformed)", bound);
        setAxesBoundaries(bound.getMinX(), bound.getMaxX(), bound.getMinY(), bound.getMaxY());
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

        fireCanvasChange(CanvasChangeEvent.MOVING);
    }

    /**
     * @param start
     * @param current
     * @return a boundary in canvas coordinate system.
     */
    private Bounds getMouseSelectBound(MouseEvent start, MouseEvent current) {
        var area = getPlottingArea();
        var x1 = Math.max(Math.min(start.getX(), current.getX()), area.getMinX());
        var x2 = Math.min(Math.max(start.getX(), current.getX()), area.getMaxX());
        var y1 = Math.max(Math.min(start.getY(), current.getY()), area.getMinY());
        var y2 = Math.min(Math.max(start.getY(), current.getY()), area.getMaxY());
        return new BoundingBox(x1, y1, x2 - x1, y2 - y1);
    }

    /*=====================*
     * canvas moving event *
     *=====================*/

    public static class CanvasChangeEvent extends InputEvent {
        public static final EventType<CanvasChangeEvent> ANY = new EventType<>(InputEvent.ANY, "CANVAS_CHANGE");
        public static final EventType<CanvasChangeEvent> MOVING = new EventType<>(ANY, "CANVAS_MOVING");
        public static final EventType<CanvasChangeEvent> SCALING = new EventType<>(ANY, "CANVAS_SCALING");

        CanvasChangeEvent(Object source, EventTarget target, EventType<CanvasChangeEvent> type) {
            super(source, target, type);
        }
    }

    private final ObjectProperty<@Nullable EventHandler<CanvasChangeEvent>> onCanvasMovingEvent = new SimpleObjectProperty<>(null);

    {
        addEventHandler(CanvasChangeEvent.MOVING, e -> {
            var handler = getOnCanvasMoving();
            if (handler != null) handler.handle(e);
        });
    }

    public final ObjectProperty<@Nullable EventHandler<CanvasChangeEvent>> onCanvasMovingEventProperty() {
        return onCanvasMovingEvent;
    }

    public final void setOnCanvasMoving(EventHandler<CanvasChangeEvent> handler) {
        onCanvasMovingEvent.set(handler);
    }

    public final @Nullable EventHandler<CanvasChangeEvent> getOnCanvasMoving() {
        return onCanvasMovingEvent.get();
    }

    private final ObjectProperty<@Nullable EventHandler<CanvasChangeEvent>> onCanvasScalingEvent = new SimpleObjectProperty<>(null);

    {
        addEventHandler(CanvasChangeEvent.SCALING, e -> {
            var handler = getOnCanvasScaling();
            if (handler != null) handler.handle(e);
        });
    }

    public final ObjectProperty<@Nullable EventHandler<CanvasChangeEvent>> onCanvasScalingEventProperty() {
        return onCanvasScalingEvent;
    }

    public final void setOnCanvasScaling(EventHandler<CanvasChangeEvent> handler) {
        onCanvasScalingEvent.set(handler);
    }

    public final @Nullable EventHandler<CanvasChangeEvent> getOnCanvasScaling() {
        return onCanvasScalingEvent.get();
    }

    /**
     *
     */
    private void fireCanvasChange(EventType<CanvasChangeEvent> type) {
        if (!isDisabled()) {
            fireEvent(new CanvasChangeEvent(this, foreground, type));
        }
    }

    /*=============*
     * touch event *
     *=============*/

    public static class DataTouchEvent extends InputEvent {
        public static final EventType<DataSelectEvent> DATA_TOUCH = new EventType<>(InputEvent.ANY, "DATA_TOUCH");

        public final Point2D point;
        public final MouseButton button;

        public DataTouchEvent(Point2D point, MouseButton button) {
            super(DATA_TOUCH);
            this.point = point;
            this.button = button;
        }
    }

    private final ObjectProperty<@Nullable EventHandler<DataTouchEvent>> onDataTouchEvent = new SimpleObjectProperty<>(null);

    public final ObjectProperty<@Nullable EventHandler<DataTouchEvent>> onDataTouchEventProperty() {
        return onDataTouchEvent;
    }

    public final void setOnDataTouch(EventHandler<DataTouchEvent> handler) {
        onDataTouchEvent.set(handler);
    }

    public final @Nullable EventHandler<DataTouchEvent> getOnDataTouch() {
        return onDataTouchEvent.get();
    }

    /**
     * @param point  a touch point in canvas coordinate system.
     * @param button
     */
    private void fireDataTouchEvent(Point2D point, MouseButton button) {
        if (!isDisabled()) {
            var handler = getOnDataTouch();
            if (handler != null) {
                var transform = getChartTransform();
                var event = new DataTouchEvent(transform.transform(point), button);
                Platform.runLater(() -> handler.handle(event));
            }
        }
    }

    /*==============*
     * select event *
     *==============*/

    public static class DataSelectEvent extends InputEvent {
        public static final EventType<DataSelectEvent> DATA_SELECT = new EventType<>(InputEvent.ANY, "DATA_SELECT");

        public final Bounds bounds;

        public DataSelectEvent(Bounds bounds) {
            super(DATA_SELECT);
            this.bounds = bounds;
        }
    }

    private final ObjectProperty<@Nullable EventHandler<DataSelectEvent>> onDataSelectEvent = new SimpleObjectProperty<>(null);

    public final ObjectProperty<@Nullable EventHandler<DataSelectEvent>> onDataSelectEventProperty() {
        return onDataSelectEvent;
    }

    public final void setOnDataSelect(EventHandler<DataSelectEvent> handler) {
        onDataSelectEvent.set(handler);
    }

    public final @Nullable EventHandler<DataSelectEvent> getOnDataSelect() {
        return onDataSelectEvent.get();
    }

    /**
     * @param bounds a boundary in canvas coordinate system.
     */
    private void fireDataSelectEvent(Bounds bounds) {
        if (!isDisabled()) {
            var handler = getOnDataSelect();
            if (handler != null) {
                var transform = getChartTransform();
                var event = new DataSelectEvent(transform.transform(bounds));
                Platform.runLater(() -> handler.handle(event));
            }
        }
    }

    /*==============================*
     * plotting area transformation *
     *==============================*/

    private static final Affine IDENTIFY = new Affine();

    public Bounds getXAxisArea() {
        return foreground.sceneToLocal(xAxis.localToScene(xAxis.getBoundsInLocal()));
    }

    public Bounds getYAxisArea() {
        return foreground.sceneToLocal(yAxis.localToScene(yAxis.getBoundsInLocal()));
    }

    public Bounds getPlottingArea() {
        // https://openjfx.io/javadoc/24/javafx.graphics/javafx/scene/doc-files/cssref.html#xychart
        var plot = chart.lookup(".chart-plot-background");
        return foreground.sceneToLocal(plot.localToScene(plot.getBoundsInLocal()));
    }

    /**
     * Get a graphic context that painting graphics on foreground.
     * Using chart coordinate system.
     *
     * @return a graphic context.
     */
    public GraphicsContext getForegroundCanvasGraphicsContext(boolean clear) {
        var gc = foreground.getGraphicsContext2D();
        gc.setTransform(IDENTIFY);
        if (clear) {
            gc.clearRect(0, 0, foreground.getWidth(), foreground.getHeight());
        }
        gc.setTransform(getCanvasTransform());
        return gc;
    }


    /**
     * Get a graphic context that painting graphics on background.
     * Using chart coordinate system.
     *
     * @return a graphic context
     */
    public GraphicsContext getBackgroundCanvasGraphicsContext(boolean clear) {
        var gc = background.getGraphicsContext2D();
        gc.setTransform(IDENTIFY);
        if (clear) {
            gc.clearRect(0, 0, background.getWidth(), background.getHeight());
        }
        gc.setTransform(getCanvasTransform());
        return gc;
    }

    /**
     * {@return an affine transform from chart to canvas coordinate system.}
     */
    public Affine getCanvasTransform() {
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

    /**
     * {@return an affine transform from canvas to chart coordinate system.}
     */
    public Affine getChartTransform() {
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


    public void setResetAxesBoundaries(double x1, double x2, double y1, double y2) {
        resetX1 = x1;
        resetX2 = x2;
        resetY1 = y1;
        resetY2 = y2;
    }

    public void resetAxesBoundaries() {
        setAxesBoundaries(resetX1, resetX2, resetY1, resetY2);
    }

    public void setAxesBoundaries(double x1, double x2, double y1, double y2) {
        setAxisBoundary(xAxis, x1, x2);
        setAxisBoundary(yAxis, y1, y2);
        fireCanvasChange(CanvasChangeEvent.SCALING);
    }

    public void setAxesEqualRatio() {
        var area = getPlottingArea();
        var w = area.getWidth();
        var h = area.getHeight();
        var x1 = xAxis.getLowerBound();
        var x2 = xAxis.getUpperBound();
        var xw = x2 - x1;
        var y1 = yAxis.getLowerBound();
        var y2 = yAxis.getUpperBound();
        var cy = (y1 + y2) / 2;
        var yh = h * xw / w;
        y1 = cy - yh / 2;
        y2 = cy + yh / 2;
        setAxisBoundary(yAxis, y1, y2);
        fireCanvasChange(CanvasChangeEvent.SCALING);
    }

    public static void setAxisBoundary(NumberAxis axis, double x1, double x2) {
        axis.setLowerBound(x1);
        axis.setUpperBound(x2);

        var px = Math.log10(x2 - x1);
        if (px - (int) px < 1e-3) --px;


        var ux = Math.pow(10, (int) px);
        if ((x2 - x1) / ux < 4) ux = Math.pow(10, (int) px - 1);
        axis.setTickUnit(ux);
    }


}
