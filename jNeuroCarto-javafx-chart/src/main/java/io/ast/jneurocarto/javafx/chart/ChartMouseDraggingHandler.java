package io.ast.jneurocarto.javafx.chart;

import javafx.event.EventHandler;

public interface ChartMouseDraggingHandler {
    boolean onChartMouseDragDetect(ChartMouseEvent e);

    void onChartMouseDragging(ChartMouseEvent p, ChartMouseEvent e);

    void onChartMouseDragDone(ChartMouseEvent e);

    static EventHandler<ChartMouseEvent> setupChartMouseDraggingHandler(InteractionXYChart chart,
                                                                        ChartMouseDraggingHandler handler) {

        class ChartMouseDraggingHandlerImpl implements EventHandler<ChartMouseEvent> {

            private ChartMouseEvent p;

            @Override
            public void handle(ChartMouseEvent e) {
                if (e.getEventType() == ChartMouseEvent.CHART_MOUSE_PRESSED) {
                    if (handler.onChartMouseDragDetect(e)) {
                        p = e;
                        e.consume();
                    }
                } else if (e.getEventType() == ChartMouseEvent.CHART_MOUSE_DRAGGED && p != null) {
                    handler.onChartMouseDragging(p, e);
                    p = e;
                    e.consume();
                } else if (e.getEventType() == ChartMouseEvent.CHART_MOUSE_RELEASED && p != null) {
                    handler.onChartMouseDragDone(e);
                    p = null;
                    e.consume();
                }
            }
        }

        var impl = new ChartMouseDraggingHandlerImpl();
        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_PRESSED, impl);
        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_DRAGGED, impl);
        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_RELEASED, impl);
        return impl;
    }


}
