package io.ast.jneurocarto.javafx.chart.cli;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.data.XY;
import io.ast.jneurocarto.javafx.chart.data.XYMarker;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseHoverHandler;
import picocli.CommandLine;

@CommandLine.Command(
    name = "hover",
    sortOptions = false,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    description = "show hover on xy points"
)
public class Hover implements Main.Content, Runnable, ChartMouseHoverHandler {

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


    @Override
    public void setup(InteractionXYChart chart) {
        this.chart = chart;

        painter = chart.getPlotting();
        markers = painter.scatter()
            .fill("black")
            .wh(3, 3)
            .graphics();

        for (int i = 0; i < 100; i++) {
            var x = Math.random() * 100;
            var y = Math.random() * 100;
            markers.addData(new XY(x, y, (Object) i));
        }
        ChartMouseHoverHandler.setupChartMouseHoverHandler(chart, markers, this);
        painter.repaint();
    }

    @Override
    public String onChartMouseHoverOn(ChartMouseEvent e, @Nullable XY data) {
        if (data == null) return null;
        return data.toString();
    }
}
