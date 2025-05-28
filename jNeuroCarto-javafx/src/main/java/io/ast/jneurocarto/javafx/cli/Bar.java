package io.ast.jneurocarto.javafx.cli;

import java.util.Arrays;

import io.ast.jneurocarto.javafx.chart.Application;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.XYBar;
import picocli.CommandLine;

@CommandLine.Command(
  name = "bar",
  usageHelpAutoWidth = true,
  description = "show bar graphics"
)
public class Bar implements Application.ApplicationContent, Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--orientation", defaultValue = "vertical")
    XYBar.Orientation orientation;

    @CommandLine.Option(names = "--color", defaultValue = "blue")
    String color;

    @CommandLine.ArgGroup(heading = "Data:%n")
    BarData gen;

    public static class BarData {
        @CommandLine.ArgGroup(exclusive = false)
        RandomData r;

        @CommandLine.Option(names = "--values",
          arity = "0..1", split = ",",
          description = "given value sequence")
        double[] v;

        public double[] get() {
            if (v != null) {
                return v;
            } else {
                return r.get();
            }
        }
    }

    public static class RandomData {
        @CommandLine.Option(names = "-n", defaultValue = "25",
          description = "N random numbers")
        int n = 25;

        @CommandLine.Option(names = "-min", defaultValue = "0",
          description = "min random number")
        int min = 0;

        @CommandLine.Option(names = "-max", defaultValue = "100",
          description = "max random numbers")
        int max = 100;

        public double[] get() {
            var ret = new double[n];
            for (int i = 0; i < n; i++) {
                ret[i] = Math.random() * (max - min) + min;
            }
            return ret;
        }
    }

    @CommandLine.ParentCommand
    public Chart parent;

    double[] data;

    @Override
    public void run() {
        data = gen != null ? gen.get() : new RandomData().get();
        System.out.println(Arrays.toString(data));

        parent.launch(new Application(this));
    }

    /*=============*
     * Application *
     *=============*/

    @Override
    public void setup(InteractionXYChart chart) {
        var painter = chart.getPlotting();
        painter.bar(data, orientation)
          .widthRatio(0.8)
          .fitInRange(0, 100)
          .fill(color);

        painter.repaint();
    }
}
