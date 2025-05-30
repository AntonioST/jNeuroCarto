package io.ast.jneurocarto.javafx.cli;

import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import io.ast.jneurocarto.javafx.chart.Application;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.XYText;
import io.ast.jneurocarto.javafx.utils.FlashStringBuffer;
import picocli.CommandLine;

@CommandLine.Command(
  name = "flash",
  usageHelpAutoWidth = true,
  description = "show flash text"
)
public class FlashText implements Application.ApplicationContent, Runnable {
    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.ParentCommand
    public Chart parent;

    private FlashStringBuffer buffer;
    private InteractionXYPainter painter;
    private XYText.Builder builder;

    @Override
    public void run() {
        parent.launch(new Application(this));
    }

    /*=============*
     * Application *
     *=============*/

    @Override
    public void setup(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyType);
    }

    @Override
    public void setup(InteractionXYChart chart) {
        buffer = new FlashStringBuffer();
        painter = chart.getForegroundPainter();
        builder = painter.text()
          .font(Font.font("monospace", 60))
          .align(TextAlignment.LEFT)
          .baseline(VPos.CENTER)
          .color(Color.BLACK);
    }

    public void onKeyType(KeyEvent e) {
        if (buffer.accept(e.getCode())) {
            e.consume();
            builder.graphics().clearData();
            var text = buffer.toString();
            if (!text.isEmpty()) {
                builder.addText(text.replaceAll(" +", "\n"), 50, 50, 10, 10);
            }
            painter.repaint();
        }
    }
}
