package io.ast.jneurocarto.javafx.app;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

@NullMarked
public class InteractXYChart<C extends XYChart<Number, Number>> extends StackPane {

    private final C chart;
    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final Canvas background;
    private final Canvas foreground;

    public InteractXYChart(C chart) {
        this.chart = chart;

        xAxis = (NumberAxis) chart.getXAxis();
        yAxis = (NumberAxis) chart.getYAxis();
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);

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
        foreground.setOnScroll(this::onMouseWheeled);
    }

    public C getChart() {
        return chart;
    }

    public static void applyStyle(XYChart.Series<Number, Number> series, String css) {
        for (var data : series.getData()) {
            var node = data.getNode();
            if (node != null) {
                node.setStyle(css);
            }
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

    private void onMouseWheeled(ScrollEvent e) {
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

    public GraphicsContext getForebroundCanvasGraphicsContext() {
        var gc = foreground.getGraphicsContext2D();
        gc.setTransform(getCanvasTransform());
        return gc;
    }

    public GraphicsContext getBackbroundCanvasGraphicsContext() {
        var gc = foreground.getGraphicsContext2D();
        gc.setTransform(getCanvasTransform());
        return gc;
    }

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

    public void resetAxesBoundaries() {
        setAxesBoundaries(0, 1000, 0, 1000);
    }

    public void setAxesBoundaries(double x1, double x2, double y1, double y2) {
        setAxisBoundary(xAxis, x1, x2);
        setAxisBoundary(yAxis, y1, y2);
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
