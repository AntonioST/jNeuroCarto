package io.ast.jneurocarto.javafx.chart;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.javafx.chart.utils.StylesheetsUtils;

@NullMarked
public class InteractionXYChart extends StackPane {

    private final ScatterChart<Number, Number> chart;
    private final Region chartPlottingArea;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final Canvas background;
    private final Canvas plotting;
    private final Canvas foreground;
    private @Nullable InteractionXYPainter painter;
    private final Canvas top;
    private final Logger log = LoggerFactory.getLogger(InteractionXYChart.class);

    public record AxesBounds(double xLower, double xUpper, double yLower, double yUpper) {
        public AxesBounds(NumberAxis x, NumberAxis y) {
            this(x.getLowerBound(), x.getUpperBound(), y.getLowerBound(), y.getUpperBound());
        }

        public void apply(NumberAxis x, NumberAxis y) {
            x.setLowerBound(xLower);
            x.setUpperBound(xUpper);
            y.setLowerBound(yLower);
            y.setUpperBound(yUpper);
        }
    }

    private AxesBounds resetBounds;

    public InteractionXYChart() {
        xAxis = new NumberAxis("", 0, 1000, 100);
        yAxis = new NumberAxis("", 0, 1000, 100);
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        resetBounds = new AxesBounds(xAxis, yAxis);
        chart = new ScatterChart<>(xAxis, yAxis);

        // https://openjfx.io/javadoc/24/javafx.graphics/javafx/scene/doc-files/cssref.html#xychart
        chartPlottingArea = (Region) chart.lookup(".chart-plot-background");
        chartPlottingArea.layoutXProperty().addListener((_, _, _) -> updateCanvasLayout());
        chartPlottingArea.layoutYProperty().addListener((_, _, _) -> updateCanvasLayout());
        chartPlottingArea.setStyle("-fx-background-color: transparent;");

        background = new Canvas();
        background.setManaged(false);
        background.widthProperty().bind(chartPlottingArea.widthProperty());
        background.heightProperty().bind(chartPlottingArea.heightProperty());

        plotting = new Canvas();
        plotting.setManaged(false);
        plotting.widthProperty().bind(chartPlottingArea.widthProperty());
        plotting.heightProperty().bind(chartPlottingArea.heightProperty());

        foreground = new Canvas();
        foreground.setManaged(false);
        foreground.widthProperty().bind(chartPlottingArea.widthProperty());
        foreground.heightProperty().bind(chartPlottingArea.heightProperty());

        top = new Canvas();
        top.setMouseTransparent(false);
        top.widthProperty().bind(chart.widthProperty());
        top.heightProperty().bind(chart.heightProperty());

        getChildren().addAll(background, chart, plotting, foreground, top);

        top.setOnMousePressed(this::onMousePressed);
        top.setOnMouseReleased(this::onMouseReleased);
        top.setOnMouseDragged(this::onMouseDragged);
        top.setOnMouseClicked(this::onMouseClicked);
        top.setOnScroll(this::onMouseWheeled);
    }

    public ScatterChart<Number, Number> getChart() {
        return chart;
    }

    public NumberAxis getXAxis() {
        return xAxis;
    }

    public NumberAxis getYAxis() {
        return yAxis;
    }

    /*=============*
     * data series *
     *=============*/


    public InteractionXYPainter getPlotting() {
        var ret = painter;
        if (ret == null) {
            painter = ret = new InteractionXYPainter(this, plotting, 0);
        }
        return ret;
    }

    public InteractionXYPainter getForegroundPainter() {
        var ret = new InteractionXYPainter(this, foreground, 1);
        addForegroundPlotting(new WeakRefPlottingJob(ret));
        return ret;
    }

    public InteractionXYPainter getBackgroundPainter() {
        var ret = new InteractionXYPainter(this, background, -1);
        addBackgroundPlotting(new WeakRefPlottingJob(ret));
        return ret;
    }

    public static Stream<XYChart.Data<Number, Number>> getVisible(XYChart.Series<Number, Number> series) {
        return series.getData().stream()
          .filter(it -> it.getNode().isVisible());
    }

    public static void setVisible(XYChart.Series<Number, Number> series, boolean visible) {
        for (XYChart.Data<Number, Number> it : series.getData()) {
            it.getNode().setVisible(visible);
        }
    }

    public static void setVisible(XYChart.Series<Number, Number> series, Predicate<@Nullable Object> tester) {
        for (XYChart.Data<Number, Number> it : series.getData()) {
            var value = it.getExtraValue();
            it.getNode().setVisible(tester.test(value));
        }
    }

    public static <T> void setVisible(XYChart.Series<Number, Number> series, Class<T> cls, Predicate<T> tester) {
        for (XYChart.Data<Number, Number> it : series.getData()) {
            var value = it.getExtraValue();
            var visible = cls.isInstance(value) && tester.test((T) value);
            it.getNode().setVisible(visible);
        }
    }


    public static boolean hasStyleClass(XYChart.Data<Number, Number> data, String css) {
        Node node;
        return (node = data.getNode()) != null && StylesheetsUtils.hasStyleClass(node, css);
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

    public static Stream<XYChart.Data<Number, Number>> filterStyleClass(XYChart.Series<Number, Number> series, String css) {
        return series.getData().stream()
          .filter(it -> hasStyleClass(it, css));
    }

    public static Stream<XYChart.Data<Number, Number>> filterAndRemoveStyleClass(XYChart.Series<Number, Number> series, String css) {
        return series.getData().stream().filter(it -> {
            var ret = hasStyleClass(it, css);
            if (ret) removeStyleClass(it, css);
            return ret;
        });
    }

    public static Stream<XYChart.Data<Number, Number>> filterInBound(XYChart.Series<Number, Number> series, Bounds bounds, boolean all) {
        return series.getData().stream().filter(it -> {
            var x = it.getXValue().doubleValue();
            var y = it.getYValue().doubleValue();
            return bounds.contains(x, y) && (all || it.getNode().isVisible());
        });
    }

    public static Stream<XYChart.Data<Number, Number>> filterInBoundAndSetStyleClass(XYChart.Series<Number, Number> series, Bounds bounds, String css, boolean all) {
        return series.getData().stream().filter(it -> {
            var x = it.getXValue().doubleValue();
            var y = it.getYValue().doubleValue();
            var r = bounds.contains(x, y) && (all || it.getNode().isVisible());
            if (r) {
                applyStyleClass(it, css);
            } else {
                removeStyleClass(it, css);
            }
            return r;
        });
    }

    public static Stream<XYChart.Data<Number, Number>> filterExtraValue(XYChart.Series<Number, Number> series, Predicate<@Nullable Object> tester) {
        Objects.requireNonNull(tester);
        return series.getData().stream()
          .filter(it -> tester.test(it.getExtraValue()));
    }

    public static <T> Stream<XYChart.Data<Number, Number>> filterExtraValue(XYChart.Series<Number, Number> series, Class<T> cls, Predicate<T> tester) {
        return series.getData().stream().filter(it -> {
            var value = it.getExtraValue();
            return cls.isInstance(value) && tester.test((T) value);
        });
    }

    public static Stream<XYChart.Data<Number, Number>> filterExtraValueAndSetStyleClass(XYChart.Series<Number, Number> series, String css, Predicate<@Nullable Object> tester) {
        Objects.requireNonNull(tester);
        return series.getData().stream().filter(it -> {
            var ret = tester.test(it.getExtraValue());
            if (ret) {
                applyStyleClass(it, css);
            } else {
                removeStyleClass(it, css);
            }
            return ret;
        });
    }

    public static <T> Stream<XYChart.Data<Number, Number>> filterExtraValueAndSetStyleClass(XYChart.Series<Number, Number> series, String css, Class<T> cls, Predicate<T> tester) {
        return series.getData().stream().filter(it -> {
            var value = it.getExtraValue();
            var ret = value != null && cls.isInstance(value) && tester.test((T) value);
            if (ret) {
                applyStyleClass(it, css);
            } else {
                removeStyleClass(it, css);
            }
            return ret;
        });
    }

    /*========*
     * events *
     *========*/

    private @Nullable MouseEvent mousePress;
    private @Nullable MouseEvent mouseMoving;
    private @Nullable AxesBounds previous;
    private @Nullable Bounds previousArea;
    private boolean isMouseMoved;
    private @Nullable List<EventHandler<DataDragEvent>> dragListener;

    private void onMousePressed(MouseEvent e) {
        mousePress = e;
        var button = e.getButton();
        if (button == MouseButton.PRIMARY) {
        } else if (button == MouseButton.SECONDARY) {
            previous = new AxesBounds(xAxis, yAxis);
            previousArea = getPlottingArea();
        }
    }

    private void onMouseDragged(MouseEvent e) {
        var previous = mouseMoving;
        mouseMoving = e;

        var start = mousePress;
        if (start != null) {
            if (previous == null) previous = start;

            switch (start.getButton()) {
            case MouseButton.PRIMARY -> {
                if (dragListener == null) {
                    dragListener = new ArrayList<>();
                    fireDataDragEvent(DataDragEvent.DRAG_START, start, previous, e, dragListener);
                    e.consume();
                } else if (dragListener.isEmpty()) {
                    onMouseSelecting(start, e);
                    e.consume();
                } else {
                    fireDataDragEvent(DataDragEvent.DRAGGING, start, previous, e, dragListener);
                    e.consume();
                }
            }
            case MouseButton.SECONDARY -> {
                if (start.isControlDown()) {
                    onMouseSelecting(start, e);
                } else {
                    onMouseDragging(start, e);
                }
                e.consume();
            }
            }
        }
    }

    private void onMouseReleased(MouseEvent e) {
        var start = mousePress;
        var moving = mouseMoving;
        isMouseMoved = start != null && (Math.abs(e.getX() - start.getX()) >= 5 || Math.abs(e.getY() - start.getY()) >= 5);

        mousePress = null;
        mouseMoving = null;
        previous = null;
        previousArea = null;

        if (start != null) {
            if (start.getButton() == MouseButton.PRIMARY && moving != null) {
                if (dragListener != null && !dragListener.isEmpty()) {
                    fireDataDragEvent(DataDragEvent.DRAG_DONE, start, moving, e, dragListener);
                    e.consume();
                } else {
                    onMouseSelected(start, e);
                    e.consume();
                }
            } else if (start.getButton() == MouseButton.SECONDARY && start.isControlDown()) {
                onMouseSelectZooming(start, e);
                e.consume();
            }
        }

        dragListener = null;

        var gc = top.getGraphicsContext2D();
        var w = top.getWidth();
        var h = top.getHeight();
        gc.clearRect(0, 0, w, h);
    }

    private void onMouseClicked(MouseEvent e) {
        if (!isMouseMoved) {
            fireDataTouchEvent(new Point2D(e.getX(), e.getY()), e.getButton());
        }
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

        if (getPlottingAreaFromTop().contains(p)) {
            scaleY = scaleX = true;
        } else if (getXAxisArea().contains(p)) {
            scaleX = true;
        } else if (getYAxisArea().contains(p)) {
            scaleY = true;
        }

        if (scaleX) {
            var x1 = xAxis.getLowerBound();
            var x2 = xAxis.getUpperBound();
            var r1 = (px) / top.getWidth();
            var d1 = (x2 - x1) * scale * r1;
            var d2 = (x2 - x1) * scale * (1 - r1);
            setAxisBoundary(xAxis, x1 - d1, x2 + d2);
        }

        if (scaleY) {
            var x1 = yAxis.getLowerBound();
            var x2 = yAxis.getUpperBound();
            var r1 = (py) / top.getHeight();
            var d1 = (x2 - x1) * scale * (1 - r1);
            var d2 = (x2 - x1) * scale * r1;
            setAxisBoundary(yAxis, x1 - d1, x2 + d2);
        }

        if (scaleX || scaleY) fireCanvasChange(CanvasChangeEvent.SCALING);
        e.consume();
    }

    private void onMouseSelecting(MouseEvent start, MouseEvent current) {
        var gc = top.getGraphicsContext2D();
        var area = getPlottingAreaFromTop();
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
        var transform = getChartTransform(getPlottingAreaFromTop());
        var bound = getMouseSelectBound(start, end);
        bound = transform.transform(bound);
        log.trace("onMouseSelectZooming {} (transformed)", bound);
        setAxesBoundaries(bound.getMinX(), bound.getMaxX(), bound.getMinY(), bound.getMaxY());
    }

    private void onMouseDragging(MouseEvent start, MouseEvent current) {
        var area = Objects.requireNonNull(previousArea);
        var dx = current.getX() - start.getX();
        var dy = current.getY() - start.getY();

        assert previous != null;
        var x1 = previous.xLower;
        var x2 = previous.xUpper;
        dx = dx * (x2 - x1) / area.getWidth();
        setAxisBoundary(xAxis, x1 - dx, x2 - dx);

        x1 = previous.yLower;
        x2 = previous.yUpper;
        dy = -dy * (x2 - x1) / area.getHeight();
        setAxisBoundary(yAxis, x1 - dy, x2 - dy);

        fireCanvasChange(CanvasChangeEvent.MOVING);
    }

    /**
     * @param start
     * @param current
     * @return a boundary in top coordinate system.
     */
    private Bounds getMouseSelectBound(MouseEvent start, MouseEvent current) {
        var area = getPlottingAreaFromTop();
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
            try {
                repaintBlocker = true;
                fireEvent(new CanvasChangeEvent(this, top, type));
            } finally {
                repaintBlocker = false;
            }
            repaint();
        }
    }


    /*=============*
     * touch event *
     *=============*/

    public static class DataTouchEvent extends InputEvent {
        public static final EventType<DataTouchEvent> DATA_TOUCH = new EventType<>(InputEvent.ANY, "DATA_TOUCH");

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
     * @param point  a touch point in top coordinate system.
     * @param button
     */
    private void fireDataTouchEvent(Point2D point, MouseButton button) {
        if (!isDisabled()) {
            var transform = getChartTransform(getPlottingAreaFromTop());
            var event = new DataTouchEvent(transform.transform(point), button);
            fireEvent(event);
        }
    }

    /*==============*
     * select event *
     *==============*/

    public static class DataSelectEvent extends InputEvent {
        public static final EventType<DataSelectEvent> DATA_SELECT = new EventType<>(InputEvent.ANY, "DATA_SELECT");

        /**
         * a boundary in chart coordinate system.
         */
        public final Bounds bounds;

        public DataSelectEvent(Bounds bounds) {
            super(DATA_SELECT);
            this.bounds = bounds;
        }
    }

    /**
     * @param bounds a boundary in top coordinate system.
     */
    private void fireDataSelectEvent(Bounds bounds) {
        if (!isDisabled()) {
            var transform = getChartTransform(getPlottingAreaFromTop());
            var event = new DataSelectEvent(transform.transform(bounds));
            fireEvent(event);
        }
    }

    /*============*
     * drag event *
     *============*/

    /// Data dragging event.
    ///
    /// Three phases:
    ///
    /// 1.  [DRAG_START][DataDragEvent#DRAG_START] fired when potential drag started. It is the only event type
    ///     that use [Node.fireEvent][Node#fireEvent(Event)]. The receiver need to setup the listener to listen
    ///     the following drag events.
    /// 2.  [DRAGGING][DataDragEvent#DRAGGING] fired when there are registered listener during
    ///     [DRAG_START][DataDragEvent#DRAG_START]. This event continuous fired when mouse keep moving.
    /// 3.  [DRAG_DONE][DataDragEvent#DRAG_DONE] fired when there are registered listener during
    ///     [DRAG_START][DataDragEvent#DRAG_START]. This event fired when mouse release the button.
    ///
    /// Examples:
    ///
    /// {@snippet file = "InteractionXYChartSnippets.java" region = "drag event setup"}
    public static class DataDragEvent extends Event {
        public static final EventType<DataDragEvent> ANY = new EventType<>(Event.ANY, "DATA_DRAG");
        public static final EventType<DataDragEvent> DRAG_START = new EventType<>(ANY, "DATA_DRAG_START");
        public static final EventType<DataDragEvent> DRAGGING = new EventType<>(ANY, "DATA_DRAGGING");
        public static final EventType<DataDragEvent> DRAG_DONE = new EventType<>(ANY, "DATA_DRAG_DONE");

        public final Point2D start;
        public final Point2D previous;
        public final Point2D current;
        private final List<EventHandler<DataDragEvent>> listener;

        DataDragEvent(EventType<DataDragEvent> type,
                      Point2D start,
                      Point2D previous,
                      Point2D current,
                      List<EventHandler<DataDragEvent>> listener) {
            super(type);
            this.start = start;
            this.previous = previous;
            this.current = current;
            this.listener = listener;
        }

        public Point2D delta() {
            return new Point2D(current.getX() - previous.getX(), current.getY() - previous.getY());
        }

        public Point2D deltaFromStart() {
            return new Point2D(current.getX() - start.getX(), current.getY() - start.getY());
        }

        public void startListen(EventHandler<DataDragEvent> handler) {
            if (getEventType() == DRAG_START) {
                listener.add(handler);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private void fireDataDragEvent(EventType<DataDragEvent> type,
                                   MouseEvent start,
                                   MouseEvent previous,
                                   MouseEvent current,
                                   List<EventHandler<DataDragEvent>> listener) {
        if (!isDisabled()) {
            var transform = getChartTransform(getPlottingAreaFromTop());
            var p1 = transform.transform(start.getX(), start.getY());
            var p2 = transform.transform(previous.getX(), previous.getY());
            var p3 = transform.transform(current.getX(), current.getY());
            var event = new DataDragEvent(type, p1, p2, p3, listener);
            if (type == DataDragEvent.DRAG_START) {
                fireEvent(event);
            } else {
                for (var handler : listener) {
                    handler.handle(event);
                }
            }
        }
    }

    /*================*
     * plotting queue *
     *================*/

    public interface PlottingJob {
        default double z() {
            return 0;
        }

        void draw(GraphicsContext gc);
    }

    private static class WeakRefPlottingJob implements PlottingJob {
        private final WeakReference<PlottingJob> reference;

        WeakRefPlottingJob(PlottingJob plotting) {
            reference = new WeakReference<>(plotting);
        }

        @Override
        public double z() {
            var ref = reference.get();
            return ref == null ? Double.POSITIVE_INFINITY : ref.z();
        }

        boolean isInvalid() {
            return reference.get() == null;
        }

        @Override
        public void draw(GraphicsContext gc) {
            var plotting = reference.get();
            if (plotting != null) plotting.draw(gc);
        }
    }

    private final List<PlottingJob> foregroundJobs = new ArrayList<>();
    private final List<PlottingJob> backgroundJobs = new ArrayList<>();
    private boolean repaintBlocker = false;

    public void addForegroundPlotting(PlottingJob job) {
        var display = job;
        if (job instanceof WeakRefPlottingJob ref) {
            display = ref.reference.get();
            if (display == null) return;
        }
        LoggerFactory.getLogger(getClass()).debug("addForegroundPlotting {}", display.getClass().getSimpleName());
        foregroundJobs.add(job);
    }

    public boolean removeForegroundPlotting(PlottingJob job) {
        var display = job;
        if (job instanceof WeakRefPlottingJob ref) {
            display = ref.reference.get();
            if (display == null) return false;
        }
        LoggerFactory.getLogger(getClass()).debug("removeForegroundPlotting {}", display.getClass().getSimpleName());
        return foregroundJobs.remove(job);
    }

    public void addBackgroundPlotting(PlottingJob job) {
        var display = job;
        if (job instanceof WeakRefPlottingJob ref) {
            display = ref.reference.get();
            if (display == null) return;
        }
        LoggerFactory.getLogger(getClass()).debug("addBackgroundPlotting {}", display.getClass().getSimpleName());
        backgroundJobs.add(job);
    }

    public boolean removeBackgroundPlotting(PlottingJob job) {
        var display = job;
        if (job instanceof WeakRefPlottingJob ref) {
            display = ref.reference.get();
            if (display == null) return false;
        }
        LoggerFactory.getLogger(getClass()).debug("removeBackgroundPlotting {}", display.getClass().getSimpleName());
        return backgroundJobs.remove(job);
    }

    void reorderPainter() {
        foregroundJobs.sort(Comparator.comparingDouble(PlottingJob::z));
        backgroundJobs.sort(Comparator.comparingDouble(PlottingJob::z));
    }

    public void repaintForeground() {
        if (repaintBlocker) return;
        var gc = getForegroundChartGraphicsContext(true);
        var iter = foregroundJobs.iterator();
        while (iter.hasNext()) {
            var job = iter.next();
            if (job instanceof WeakRefPlottingJob w && w.isInvalid()) {
                iter.remove();
            } else {
                job.draw(gc);
            }
        }
    }

    public void repaintBackground() {
        if (repaintBlocker) return;
        var gc = getBackgroundChartGraphicsContext(true);
        var iter = backgroundJobs.iterator();
        while (iter.hasNext()) {
            var job = iter.next();
            if (job instanceof WeakRefPlottingJob w && w.isInvalid()) {
                iter.remove();
            } else {
                job.draw(gc);
            }
        }
    }

    public void repaint() {
        if (repaintBlocker) return;
        repaintBackground();
        var data = painter;
        if (data != null) data.repaint();
        repaintForeground();
    }

    /*==============================*
     * plotting area transformation *
     *==============================*/


    private static final Affine IDENTIFY = new Affine();

    public AxesBounds getAxesBounds() {
        return new AxesBounds(xAxis, yAxis);
    }

    public Bounds getXAxisArea() {
        return top.sceneToLocal(xAxis.localToScene(xAxis.getBoundsInLocal()));
    }

    public Bounds getYAxisArea() {
        return top.sceneToLocal(yAxis.localToScene(yAxis.getBoundsInLocal()));
    }

    private Bounds getPlottingAreaFromTop() {
        return top.sceneToLocal(chartPlottingArea.localToScene(chartPlottingArea.getBoundsInLocal()));
    }

    public Bounds getPlottingArea() {
        // both foreground and chartPlottingArea have same bounds.
        return chartPlottingArea.getBoundsInLocal();
    }

    /**
     * Get a graphic context that painting graphics on foreground.
     * Using chart coordinate system.
     *
     * @return a graphic context.
     */
    public GraphicsContext getForegroundChartGraphicsContext(boolean clear) {
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
    public GraphicsContext getBackgroundChartGraphicsContext(boolean clear) {
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
     * transform a chart point to a canvas point.
     *
     * @param p a point in chart coordinate system
     * @return a point in canvas coordinate system
     */
    public Point2D getCanvasTransform(Point2D p) {
        return getCanvasTransform(p.getX(), p.getY());
    }

    /**
     * transform chart point to canvas point.
     *
     * @param px a x position of a point in chart coordinate system
     * @param py a y position of a point in chart coordinate system
     * @return a point in canvas coordinate system
     */
    public Point2D getCanvasTransform(double px, double py) {
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

        var nx = mxx * px + mxy * py + mxt;
        var ny = myx * px + myy * py + myt;
        return new Point2D(nx, ny);
    }

    /**
     * transform chart length to canvas length.
     *
     * @param vw a x-axis-directed length in chart coordinate system
     * @param vh a y-axis-directed length in chart coordinate system
     * @return a directed in canvas coordinate system.
     * {@link Point2D#getX()} means x-axis-directed length; and
     * {@link Point2D#getY()} means y-axis-directed length
     */
    public Point2D getCanvasTransformScaling(double vw, double vh) {
        var ax = xAxis;
        var ay = yAxis;
        var area = getPlottingArea();
        var w = ax.getUpperBound() - ax.getLowerBound();
        var h = ay.getUpperBound() - ay.getLowerBound();

        var mxx = area.getWidth() / w;
        var mxy = 0;
        var myx = 0;
        var myy = -area.getHeight() / h;

        var nx = mxx * vw + mxy * vh;
        var ny = myx * vw + myy * vh;
        return new Point2D(nx, ny);
    }

    /**
     * {@return an affine transform from canvas to chart coordinate system.}
     */
    public Affine getChartTransform() {
        return getChartTransform(getPlottingArea());
    }

    /**
     * transform a canvas point to a chart point.
     *
     * @param p a point in canvas coordinate system
     * @return a point in chart coordinate system
     */
    public Point2D getChartTransform(Point2D p) {
        return getChartTransform(p.getX(), p.getY());
    }

    /**
     * transform canvas point to chart point.
     *
     * @param px a x position of a point in canvas coordinate system
     * @param py a y position of a point in canvas coordinate system
     * @return a point in chart coordinate system
     */
    public Point2D getChartTransform(double px, double py) {
        return getChartTransform(px, py, getPlottingArea());
    }

    /**
     * transform canvas length to chart length.
     *
     * @param vw a x-axis-directed length in canvas coordinate system
     * @param vh a y-axis-directed length in canvas coordinate system
     * @return a directed in chart coordinate system.
     * {@link Point2D#getX()} means x-axis-directed length; and
     * {@link Point2D#getY()} means y-axis-directed length
     */
    public Point2D getChartTransformScaling(double vw, double vh) {
        return getChartTransformScaling(vw, vh, getPlottingArea());
    }

    /**
     * {@return an affine transform from scene to chart coordinate system.}
     */
    public Affine getChartTransformFromScene() {
        var area = chartPlottingArea.localToScene(chartPlottingArea.getBoundsInLocal());
        return getChartTransform(area);
    }

    /**
     * transform a scene point to a chart point.
     *
     * @param p a point on scene
     * @return a point in chart coordinate system
     */
    public Point2D getChartTransformFromScene(Point2D p) {
        return getChartTransformFromScene(p.getX(), p.getY());
    }

    /**
     * transform scene point to chart point.
     *
     * @param px a x position of a point on scene
     * @param py a y position of a point on scene
     * @return a point in chart coordinate system
     */
    public Point2D getChartTransformFromScene(double px, double py) {
        var area = chartPlottingArea.localToScene(chartPlottingArea.getBoundsInLocal());
        return getChartTransform(px, py, area);
    }

    /**
     * transform scene length to chart length.
     *
     * @param vw a x-axis-directed length on scene
     * @param vh a y-axis-directed length on scene
     * @return a directed in chart coordinate system.
     * {@link Point2D#getX()} means x-axis-directed length; and
     * {@link Point2D#getY()} means y-axis-directed length
     */
    public Point2D getChartTransformScalingFromScene(double vw, double vh) {
        var area = chartPlottingArea.localToScene(chartPlottingArea.getBoundsInLocal());
        return getChartTransformScaling(vw, vh, area);
    }

    /**
     * {@return an affine transform from area's coordinate to chart coordinate system.}
     */
    private Affine getChartTransform(Bounds area) {
        var ax = xAxis;
        var ay = yAxis;
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

    /**
     * transform an area's  point to a chart point.
     *
     * @param p a point in area's coordinate system
     * @return a point in chart coordinate system
     */
    private Point2D getChartTransform(Point2D p, Bounds area) {
        return getChartTransformFromScene(p.getX(), p.getY());
    }

    /**
     * transform an area's point to chart point.
     *
     * @param px a x position of a point in area's coordinate system
     * @param py a y position of a point in area's coordinate system
     * @return a point in chart coordinate system
     */
    public Point2D getChartTransform(double px, double py, Bounds area) {
        var ax = xAxis;
        var ay = yAxis;
        var w = ax.getUpperBound() - ax.getLowerBound();
        var h = ay.getUpperBound() - ay.getLowerBound();

        var mxx = w / area.getWidth();
        var mxy = 0;
        var mxt = ax.getLowerBound() - area.getMinX() * w / area.getWidth();
        var myx = 0;
        var myy = -h / area.getHeight();
        var myt = ay.getLowerBound() + area.getMaxY() * h / area.getHeight();

        var nx = mxx * px + mxy * py + mxt;
        var ny = myx * px + myy * py + myt;
        return new Point2D(nx, ny);
    }

    /**
     * transform area's length to chart length.
     *
     * @param vw a x-axis-directed length in area's coordinate system
     * @param vh a y-axis-directed length in area's coordinate system
     * @return a directed in chart coordinate system.
     * {@link Point2D#getX()} means x-axis-directed length; and
     * {@link Point2D#getY()} means y-axis-directed length
     */
    public Point2D getChartTransformScaling(double vw, double vh, Bounds area) {
        var ax = xAxis;
        var ay = yAxis;
        var w = ax.getUpperBound() - ax.getLowerBound();
        var h = ay.getUpperBound() - ay.getLowerBound();

        var mxx = w / area.getWidth();
        var mxy = 0;
        var myx = 0;
        var myy = -h / area.getHeight();

        var nx = mxx * vw + mxy * vh;
        var ny = myx * vw + myy * vh;
        return new Point2D(nx, ny);
    }

    public AxesBounds getResetAxesBoundaries() {
        return resetBounds;
    }

    public void setResetAxesBoundaries(AxesBounds bounds) {
        resetBounds = bounds;
    }

    public void setResetAxesBoundaries(double x1, double x2, double y1, double y2) {
        resetBounds = new AxesBounds(x1, x2, y1, y2);
    }

    public void resetAxesBoundaries() {
        setAxesBoundaries(resetBounds);
    }

    public void setAxesBoundaries(AxesBounds bounds) {
        setAxesBoundaries(bounds.xLower, bounds.xUpper, bounds.yLower, bounds.yUpper);
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
        var px = Math.log10(x2 - x1);
        if (px - (int) px < 1e-3) --px;

        var ux = Math.pow(10, (int) px);
        if ((x2 - x1) / ux < 4) ux = Math.pow(10, (int) px - 1);

        x1 = Math.round(x1 / ux * 100) * ux / 100;
        x2 = Math.round(x2 / ux * 100) * ux / 100;

        axis.setLowerBound(x1);
        axis.setUpperBound(x2);
        axis.setTickUnit(ux);
    }

    private void updateCanvasLayout() {
        var a = chartPlottingArea;
        var b = sceneToLocal(a.localToScene(a.getBoundsInLocal()));
        var x = b.getMinX();
        var y = b.getMinY();

        foreground.relocate(x, y);
        plotting.relocate(x, y);
        background.relocate(x, y);
    }

}
