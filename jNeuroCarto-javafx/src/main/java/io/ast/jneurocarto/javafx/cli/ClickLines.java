package io.ast.jneurocarto.javafx.cli;

import javafx.scene.chart.ScatterChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import io.ast.jneurocarto.javafx.chart.*;
import picocli.CommandLine;

@CommandLine.Command(
  name = "click",
  usageHelpAutoWidth = true,
  description = "show interaction xy chart"
)
public class ClickLines implements Application.ApplicationContent, Runnable {

    @CommandLine.ParentCommand
    public Chart parent;

    @Override
    public void run() {
        parent.launch(new Application(this));
    }

    /*=============*
     * Application *
     *=============*/

    private InteractionXYChart<ScatterChart<Number, Number>> chart;
    private InteractionXYPainter painter;
    private XYPath path;

    @Override
    public void setup(InteractionXYChart<ScatterChart<Number, Number>> chart) {
        this.chart = chart;
        chart.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseClicked);

        painter = chart.getPlotting();

        path = new XYPath();
        painter.addGraphics(path);

        path.line(Color.TRANSPARENT);
        path.normalize(Normalize.N01);
        path.colormap("jet");
        path.addData(0, 0, 0);
    }

    private void onMouseClicked(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            if (e.getClickCount() >= 2) {
                chart.resetAxesBoundaries();
            } else {
                var p = chart.getChartTransformFromScene(e.getSceneX(), e.getSceneY());
                path.addData(p, Math.random());
                painter.repaint();
            }
        }
    }
}
