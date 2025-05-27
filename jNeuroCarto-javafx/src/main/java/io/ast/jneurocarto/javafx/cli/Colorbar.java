package io.ast.jneurocarto.javafx.cli;

import javafx.scene.chart.ScatterChart;

import io.ast.jneurocarto.javafx.chart.Application;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XYMatrix;
import picocli.CommandLine;

@CommandLine.Command(
  name = "colorbar",
  usageHelpAutoWidth = true,
  description = "show colorbar"
)
public class Colorbar implements Application.ApplicationContent, Runnable {

    @CommandLine.Parameters(index = "0", defaultValue = "jet",
      description = "colormap")
    String colormap;

    @CommandLine.Parameters(index = "1", defaultValue = "25",
      description = "colormap N")
    int n;

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
    private XYMatrix colorbar;

    @Override
    public void setup(InteractionXYChart<ScatterChart<Number, Number>> chart) {
        this.chart = chart;
        painter = chart.getPlotting();

        colorbar = new XYMatrix();
        painter.addGraphics(colorbar);

        colorbar.colormap(colormap);
        colorbar.normalize(0, n);
        colorbar.x(0);
        colorbar.y(0);
        colorbar.w(100);
        colorbar.h(10);

        for (int i = 0; i < n + 1; i++) {
            colorbar.addData(i, 0, i);
        }

        painter.repaint();
    }
}
