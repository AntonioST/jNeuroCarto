package io.ast.jneurocarto.javafx.chart.snippets;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XYBar;

class BarExample {

    public void barExample(InteractionXYChart chart) {
        // @start region="bar example"
        InteractionXYPainter painter = chart.getForegroundPainter();
        double[] data = new double[10]; // @replace regex="(?<== ).*" replacement="..."
        painter.bar(data)
          .fill("red");
        // @end
    }

    public void stackBarExample(InteractionXYChart chart) {
        // @start region="stack bar example"
        InteractionXYPainter painter = chart.getForegroundPainter();
        double[][] data = new double[3][];
        // fill data array
        var bar = painter.bar(data[0], XYBar.Orientation.vertical).baseline(0);
        for (int i = 1; i < data.length; i++) {
            bar = painter.bar(data[i], XYBar.Orientation.vertical).stackOn(bar)
              .fill("color"); // remember to set different color // @replace substring="color" replacement="..."
        }
        // @end
        // @start region="normalized stack bar example"
        bar.normalizeStack(100);
        // @end
    }
}