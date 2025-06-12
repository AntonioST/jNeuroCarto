package io.ast.jneurocarto.javafx.chart.event;


import java.util.Objects;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import javafx.scene.transform.NonInvertibleTransformException;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.data.XY;
import io.ast.jneurocarto.javafx.chart.data.XYSeries;

public interface ChartMouseHoverHandler {
    String onChartMouseHoverOn(ChartMouseEvent e, @Nullable XY data);

    default Point2D onHoverTooltipShowing(Tooltip tooltip, Point2D anchor) {
        var height = tooltip.getHeight();
        return new Point2D(anchor.getX() + 10, anchor.getY() - height / 2);
    }

    static EventHandler<ChartMouseEvent> setupChartMouseHoverHandler(InteractionXYChart chart,
                                                                     XYSeries series,
                                                                     ChartMouseHoverHandler handler) {
        class ChartMouseHoverHandlerImpl implements EventHandler<ChartMouseEvent> {
            private Tooltip tooltip = new Tooltip();

            @Override
            public void handle(ChartMouseEvent event) {
                var data = series.touch(event.point);
                if (data == null) {
                    tooltip.hide();
                } else {
                    var text = handler.onChartMouseHoverOn(event, data);
                    if (text == null) {
                        tooltip.hide();
                    } else if (!Objects.equals(tooltip.getText(), text) || !tooltip.isShowing()) {
                        tooltip.setText(text);
                        getHeight();

                        var p = getAnchor(event, data);
                        p = handler.onHoverTooltipShowing(tooltip, p);

                        tooltip.show(chart, p.getX(), p.getY());
                    }
                }
            }

            private double getHeight() {
                var h = tooltip.getHeight();
                if (h == 0) {
                    tooltip.show(chart, 0, 0);
                    tooltip.hide();
                    h = tooltip.getHeight();
                }
                return h;
            }

            private Point2D getAnchor(ChartMouseEvent event, XY data) {
                try {
                    return event.chartTransformFromScreen().inverseTransform(data.x(), data.y());
                } catch (NonInvertibleTransformException e) {
                    var mouse = event.mouse;
                    return new Point2D(mouse.getScreenX(), mouse.getScreenY());
                }
            }
        }

        var impl = new ChartMouseHoverHandlerImpl();
        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_MOVED, impl);
        return impl;
    }
}
