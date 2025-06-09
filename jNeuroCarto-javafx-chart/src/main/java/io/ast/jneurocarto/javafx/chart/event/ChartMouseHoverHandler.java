package io.ast.jneurocarto.javafx.chart.event;


import javafx.event.EventHandler;
import javafx.scene.control.Tooltip;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.data.XY;
import io.ast.jneurocarto.javafx.chart.data.XYSeries;

public interface ChartMouseHoverHandler {
    String onChartMouseHoverOn(ChartMouseEvent e, @Nullable XY data);

    default void onHoverTooltipShowing(Tooltip tooltip) {
    }

    static EventHandler<ChartMouseEvent> setupChartMouseHoverHandler(InteractionXYChart chart,
                                                                     XYSeries series,
                                                                     ChartMouseHoverHandler handler) {
        class ChartMouseHoverHandlerImpl implements EventHandler<ChartMouseEvent> {
            private Tooltip tooltip = new Tooltip();

            @Override
            public void handle(ChartMouseEvent event) {
                var data = series.touch(event.point);
                var text = handler.onChartMouseHoverOn(event, data);
                if (text == null) {
                    tooltip.hide();
                } else {
                    tooltip.setText(text);
                    handler.onHoverTooltipShowing(tooltip);
                    var mouse = event.mouse;
                    var height = tooltip.getHeight() / 2;
                    tooltip.show(chart, mouse.getScreenX() + 2, mouse.getScreenY() - height);
                }
            }
        }

        var impl = new ChartMouseHoverHandlerImpl();
        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_MOVED, impl);
        return impl;
    }
}
