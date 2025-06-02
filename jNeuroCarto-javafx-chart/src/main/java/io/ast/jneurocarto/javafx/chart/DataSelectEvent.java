package io.ast.jneurocarto.javafx.chart;

import javafx.event.EventType;
import javafx.geometry.Bounds;
import javafx.scene.input.InputEvent;

public class DataSelectEvent extends InputEvent {
    public static final EventType<DataSelectEvent> DATA_SELECT = new EventType<>(InputEvent.ANY, "DATA_SELECT");

    /**
     * a selection boundary in chart coordinate.
     */
    public final Bounds bounds;

    public DataSelectEvent(Bounds bounds) {
        super(DATA_SELECT);
        this.bounds = bounds;
    }
}
