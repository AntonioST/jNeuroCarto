package io.ast.jneurocarto.javafx.chart.snippets;


import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.XY;
import io.ast.jneurocarto.javafx.chart.XYSeries;

@NullMarked
public class InteractionXYChartSnippets {

    public class DragEvent {
        // @start region="drag event setup"
        private XYSeries data;
        private List<XY> selected = new ArrayList<>();

        public void setup(InteractionXYChart chart) {
            chart.addEventHandler(InteractionXYChart.DataSelectEvent.DATA_SELECT, this::onSelected);
            chart.addEventHandler(InteractionXYChart.DataDragEvent.DRAG_START, this::onStartDrag);
        }

        private void onSelected(InteractionXYChart.DataSelectEvent e) {
            selected.addAll(data.touch(e.bounds)); // check which data are selected.
        }

        private void onStartDrag(InteractionXYChart.DataDragEvent e) {
            if (!selected.isEmpty()) {
                e.startListen(this::onDragging);
            }
        }

        private void onDragging(InteractionXYChart.DataDragEvent e) {
            if (e.getEventType() == InteractionXYChart.DataDragEvent.DRAGGING) {
                // do something
            } else {
                selected.clear();
            }
        }
        // @end
    }
}
