package io.ast.jneurocarto.javafx.chart.cli;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XY;
import io.ast.jneurocarto.javafx.chart.XYMarker;
import picocli.CommandLine;

@CommandLine.Command(
    name = "drag-drop",
    usageHelpAutoWidth = true,
    description = "show interaction xy chart for drag-and-drop feature"
)
public class DragDrop implements Main.Content, Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.ParentCommand
    public Main parent;

    @Override
    public void run() {
        parent.launch(this);
    }

    /*=============*
     * Application *
     *=============*/

    private InteractionXYChart chart;
    private InteractionXYPainter painter;
    private XYMarker markers;
    private InteractionXYChart.ChartMouseEvent prev;
    private List<XY> selected = new ArrayList<>();

    @Override
    public void setup(InteractionXYChart chart) {
        this.chart = chart;
        chart.addEventHandler(InteractionXYChart.DataSelectEvent.DATA_SELECT, this::onSelected);
        chart.addEventHandler(InteractionXYChart.ChartMouseEvent.CHART_MOUSE_CLICKED, this::onClicked);
        chart.addEventHandler(InteractionXYChart.ChartMouseEvent.CHART_MOUSE_DRAGGED, this::onDragging);
        chart.addEventHandler(InteractionXYChart.ChartMouseEvent.CHART_MOUSE_RELEASED, this::onDragging);

        painter = chart.getPlotting();
        markers = painter.scatter()
            .fill(Color.BLACK)
            .wh(5, 5)
            .graphics();
    }

    private void onClicked(InteractionXYChart.ChartMouseEvent e) {
        markers.addData(e.point);
        painter.repaint();
    }

    private void onSelected(InteractionXYChart.DataSelectEvent e) {
        selected.addAll(markers.touch(e.bounds));
        if (!selected.isEmpty()) {
            System.out.println("select " + selected.stream() + " points");
            e.consume();
        }
    }

    private void onDragging(InteractionXYChart.ChartMouseEvent e) {
        if (e.getEventType() == InteractionXYChart.ChartMouseEvent.CHART_MOUSE_DRAGGED && !selected.isEmpty()) {
            System.out.println("dragging " + selected.size() + " points");
            if (prev != null) {

                var dx = e.getChartX() - prev.getChartX();
                var dy = e.getChartY() - prev.getChartY();
                for (var xy : selected) {
                    xy.x(xy.x() + dx);
                    xy.y(xy.y() + dy);
                }
                painter.repaint();
            }
            prev = e;
            e.consume();
        } else if (e.getEventType() == InteractionXYChart.ChartMouseEvent.CHART_MOUSE_RELEASED && prev != null) {
            System.out.println("release " + selected.size() + " points");
            selected.clear();
            prev = null;
        }
    }

}
