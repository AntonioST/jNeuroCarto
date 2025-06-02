package io.ast.jneurocarto.javafx.chart;

import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.InputEvent;

public class ChartChangeEvent extends InputEvent {
    public static final EventType<ChartChangeEvent> ANY = new EventType<>(InputEvent.ANY, "CANVAS_CHANGE");
    public static final EventType<ChartChangeEvent> MOVING = new EventType<>(ANY, "CANVAS_MOVING");
    public static final EventType<ChartChangeEvent> SCALING = new EventType<>(ANY, "CANVAS_SCALING");

    ChartChangeEvent(Object source, EventTarget target, EventType<ChartChangeEvent> type) {
        super(source, target, type);
    }
}
