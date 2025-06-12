package io.ast.jneurocarto.javafx.chart.cli;

import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.colormap.Normalize;
import io.ast.jneurocarto.javafx.chart.data.XYPath;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import picocli.CommandLine;

@CommandLine.Command(
    name = "click",
    sortOptions = false,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    description = "show interaction xy chart"
)
public class ClickLines implements Main.Content, Runnable {

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
        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_CLICKED, this::onMouseClicked);

        painter = chart.getPlotting();
        path = painter.lines()
            .line(Color.TRANSPARENT)
            .colormap("jet")
            .normalize(Normalize.N01)
            .addPoint(0, 0, 0)
            .graphics();
    }

    private void onMouseClicked(ChartMouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            if (e.getClickCount() >= 2) {
                chart.resetAxesBoundaries();
            } else {
                path.addData(e.point, Math.random());
                painter.repaint();
            }
        }
    }
}
