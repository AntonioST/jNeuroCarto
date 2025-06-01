package io.ast.jneurocarto.javafx.cli;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.javafx.chart.*;
import picocli.CommandLine;

@CommandLine.Command(
  name = "drag-drop",
  usageHelpAutoWidth = true,
  description = "show interaction xy chart for drag-and-drop feature"
)
public class DragDrop implements Application.ApplicationContent, Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.ParentCommand
    public Chart parent;

    @Override
    public void run() {
        parent.launch(new Application(this));
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
        chart.addEventHandler(InteractionXYChart.DataTouchEvent.DATA_TOUCH, this::onClicked);
        chart.addEventHandler(InteractionXYChart.DataSelectEvent.DATA_SELECT, this::onSelected);
        chart.addEventHandler(InteractionXYChart.DataDragEvent.DRAG_START, this::onStartDrag);

        painter = chart.getPlotting();
        markers = painter.scatter()
          .fill(Color.BLACK)
          .wh(5, 5)
          .graphics();
    }

    private void onClicked(InteractionXYChart.DataTouchEvent e) {
        markers.addData(e.point);
        painter.repaint();
    }

    private void onSelected(InteractionXYChart.DataSelectEvent e) {
        selected.addAll(markers.touch(e.bounds));
        if (!selected.isEmpty()) {
            e.consume();
        }
    }

    private void onStartDrag(InteractionXYChart.DataDragEvent e) {
        if (!selected.isEmpty()) {
            e.startListen(this::onDragging);
        }
    }

    private void onDragging(InteractionXYChart.DataDragEvent e) {
        if (e.getEventType() == InteractionXYChart.DataDragEvent.DRAGGING) {
            var d = e.delta();
            var dx = d.getX();
            var dy = d.getY();
            for (var xy : selected) {
                xy.x(xy.x() + dx);
                xy.y(xy.y() + dy);
            }
            painter.repaint();
        } else {
            selected.clear();
        }
    }
}
