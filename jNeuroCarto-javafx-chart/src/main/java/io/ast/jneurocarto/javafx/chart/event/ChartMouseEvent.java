package io.ast.jneurocarto.javafx.chart.event;

import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Affine;

public class ChartMouseEvent extends InputEvent {
    public static final EventType<ChartMouseEvent> ANY = new EventType<>(InputEvent.ANY, "CHART_MOUSE_ANY");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_CLICKED = new EventType<>(ANY, "CHART_MOUSE_CLICKED");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_PRESSED = new EventType<>(ANY, "CHART_MOUSE_PRESSED");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_RELEASED = new EventType<>(ANY, "CHART_MOUSE_RELEASED");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_MOVED = new EventType<>(ANY, "CHART_MOUSE_MOVED");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_DRAGGED = new EventType<>(ANY, "CHART_MOUSE_DRAGGED");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_ENTERED = new EventType<>(ANY, "CHART_MOUSE_ENTERED");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_EXITED = new EventType<>(ANY, "CHART_MOUSE_EXITED");
    public static final EventType<ChartMouseEvent> CHART_MOUSE_OTHER = new EventType<>(ANY, "CHART_MOUSE_OTHER");

    /**
     * mouse point in chart coordinate.
     */
    public final Point2D point;

    /**
     * origin mouse event
     */
    public final MouseEvent mouse;

    private final Affine chartTransform;

    public ChartMouseEvent(EventType<ChartMouseEvent> type,
                           Point2D point,
                           MouseEvent e,
                           Affine chartTransform) {
        super(type);
        this.point = point;
        this.mouse = e;
        this.chartTransform = chartTransform;
    }

    public double getChartX() {
        return point.getX();
    }

    public double getChartY() {
        return point.getY();
    }

    public double getMouseX() {
        return mouse.getX();
    }

    public double getMouseY() {
        return mouse.getY();
    }

    public double getMouseSceneX() {
        return mouse.getSceneX();
    }

    public double getMouseSceneY() {
        return mouse.getSceneY();
    }

    public double getMouseScreenX() {
        return mouse.getScreenX();
    }

    public double getMouseScreenY() {
        return mouse.getScreenY();
    }

    /**
     * {@return a transformation from mouse position to chart}
     */
    public Affine chartTransform() {
        return chartTransform;
    }

    /**
     * {@return a transformation from scene position to chart}
     */
    public Affine chartTransformFromScene() {
        var dx = mouse.getX() - mouse.getSceneX();
        var dy = mouse.getY() - mouse.getSceneY();
        var ret = new Affine(chartTransform);
        ret.appendTranslation(dx, dy);
        return ret;
    }

    /**
     * {@return a transformation from screen position to chart}
     */
    public Affine chartTransformFromScreen() {
        var dx = mouse.getX() - mouse.getScreenX();
        var dy = mouse.getY() - mouse.getScreenY();
        var ret = new Affine(chartTransform);
        ret.appendTranslation(dx, dy);
        return ret;
    }

    public MouseButton getButton() {
        return mouse.getButton();
    }

    public int getClickCount() {
        return mouse.getClickCount();
    }

    /**
     * @param p previous event
     * @return mouse movement in chart coordinate
     */
    public Point2D delta(ChartMouseEvent p) {
        return new Point2D(getChartX() - p.getChartX(), getChartY() - p.getChartY());
    }

    @Override
    public void consume() {
        super.consume();
        mouse.consume();
    }

}
