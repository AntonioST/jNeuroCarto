package io.ast.jneurocarto.javafx.chart.cli;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import picocli.CommandLine;

@CommandLine.Command(
  name = "colorbar",
  usageHelpAutoWidth = true,
  description = "show colorbar"
)
public class Colorbar implements Main.Content, Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Parameters(index = "0", defaultValue = "jet",
      description = "colormap")
    String colormap;

    @CommandLine.Parameters(index = "1", defaultValue = "25",
      description = "colormap N")
    int n;

    @CommandLine.ParentCommand
    public Main parent;

    @Override
    public void run() {
        parent.launch(this);
    }

    /*=============*
     * Application *
     *=============*/

    @Override
    public void setup(InteractionXYChart chart) {
        var painter = chart.getPlotting();

        var data = new double[n + 1];
        for (int i = 0; i < n + 1; i++) {
            data[i] = i;
        }

        painter.imshow(data, 1)
          .colormap(colormap)
          .normalize(0, n)
          .extent(0, 0, 100, 10);

        painter.repaint();
    }
}
