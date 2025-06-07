package io.ast.jneurocarto.javafx.chart.cli;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.data.XY;
import io.ast.jneurocarto.javafx.chart.data.XYMarker;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseDraggingHandler;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import io.ast.jneurocarto.javafx.chart.event.DataSelectEvent;
import picocli.CommandLine;

@CommandLine.Command(
    name = "drag-drop",
    sortOptions = false,
    usageHelpAutoWidth = true,
    description = "show interaction xy chart for drag-and-drop feature"
)
public class DragDrop implements Main.Content, Runnable, ChartMouseDraggingHandler {

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
    private List<XY> selected = new ArrayList<>();

    @Override
    public void setup(InteractionXYChart chart) {
        this.chart = chart;
        chart.addEventHandler(DataSelectEvent.DATA_SELECT, this::onSelected);
        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_CLICKED, this::onClicked);
        ChartMouseDraggingHandler.setupChartMouseDraggingHandler(chart, this);

        painter = chart.getPlotting();
        markers = painter.scatter()
            .fill(Color.BLACK)
            .wh(5, 5)
            .graphics();
    }

    private void onClicked(ChartMouseEvent e) {
        markers.addData(e.point);
        painter.repaint();
    }

    private void onSelected(DataSelectEvent e) {
        selected.addAll(markers.touch(e.bounds));
        if (!selected.isEmpty()) {
            System.out.println("select " + selected.size() + " points");
            e.consume();
        }
    }

    @Override
    public boolean onChartMouseDragDetect(ChartMouseEvent e) {
        if (!selected.isEmpty()) {
            System.out.println("dragging " + selected.size() + " points");
            return true;
        }
        return false;
    }

    @Override
    public void onChartMouseDragging(ChartMouseEvent p, ChartMouseEvent e) {
        var d = e.delta(p);
        var dx = d.getX();
        var dy = d.getY();
        for (var xy : selected) {
            xy.x(xy.x() + dx);
            xy.y(xy.y() + dy);
        }
        painter.repaint();
    }

    @Override
    public void onChartMouseDragDone(ChartMouseEvent e) {
        System.out.println("release " + selected.size() + " points");
        selected.clear();
    }
}
