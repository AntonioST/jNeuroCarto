package io.ast.jneurocarto.javafx.chart.cli;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.Normalize;
import io.ast.jneurocarto.javafx.chart.XYPath;
import picocli.CommandLine;

@CommandLine.Command(
  name = "click",
  usageHelpAutoWidth = true,
  description = "show interaction xy chart"
)
public class ClickLines implements Main.Content, Runnable {

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
    private XYPath path;

    @Override
    public void setup(InteractionXYChart chart) {
        this.chart = chart;
        chart.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseClicked);

        painter = chart.getPlotting();
        path = painter.lines()
          .line(Color.TRANSPARENT)
          .colormap("jet")
          .normalize(Normalize.N01)
          .addPoint(0, 0, 0)
          .graphics();
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
