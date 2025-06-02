package io.ast.jneurocarto.javafx.cli;

import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.data.XY;
import io.ast.jneurocarto.javafx.chart.data.XYText;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseDraggingHandler;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import io.ast.jneurocarto.javafx.utils.FlashStringBuffer;
import picocli.CommandLine;

@CommandLine.Command(
    name = "flash",
    usageHelpAutoWidth = true,
    description = "show flash text"
)
public class FlashText implements Example.Content, Runnable, ChartMouseDraggingHandler {
    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = {"-s", "--size"}, defaultValue = "40")
    public int fontSize;

    @CommandLine.Option(names = {"-sa", "--show-anchor"})
    public boolean showAnchor;

    @CommandLine.Option(names = {"-sb", "--show-bounds"},
        description = "draw text boundary")
    public boolean showBounds;

    @CommandLine.Option(names = {"-e", "--effect"},
        description = "use a shadow effect")
    public boolean useEffect;

    @CommandLine.Option(names = {"-n", "--newline"},
        description = "white space as newline")
    public boolean newline;

    @CommandLine.ParentCommand
    public Example parent;

    private FlashStringBuffer buffer;
    private InteractionXYPainter painter;
    private XYText.Builder builder;

    @Override
    public void run() {
        parent.launch(this);
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
            .font(Font.font("monospace", fontSize))
            .align(TextAlignment.LEFT)
            .baseline(VPos.CENTER)
            .color(Color.BLACK)
            .showAnchorPoint(showAnchor)
            .showTextBounds(showBounds);

        if (useEffect) {
            builder.textEffect(new DropShadow(BlurType.ONE_PASS_BOX, Color.GRAY, 5, 5, 5, 5));
        }

        ChartMouseDraggingHandler.setupChartMouseDraggingHandler(chart, this);
    }

    private void onKeyType(KeyEvent e) {
        if (buffer.accept(e.getCode())) {
            e.consume();
            builder.graphics().clearData();
            var text = buffer.toString();
            if (!text.isEmpty()) {
                if (newline) {
                    text = text.replaceAll(" +", "\n");
                }
                builder.addText(text, 50, 50, 0, 0);
            }
            painter.repaint();
        }
    }


    private XY touched;

    @Override
    public boolean onChartMouseDragDetect(ChartMouseEvent e) {
        var xy = builder.graphics().touch(e.point);
        if (xy != null && xy.external() instanceof String text) {
            System.out.println(text);
            touched = xy;
        } else {
            touched = null;
        }
        return touched != null;
    }

    @Override
    public void onChartMouseDragging(ChartMouseEvent p, ChartMouseEvent e) {
        touched.x(touched.x() + e.getChartX() - p.getChartX());
        touched.y(touched.y() + e.getChartY() - p.getChartY());
        painter.repaint();
    }

    @Override
    public void onChartMouseDragDone(ChartMouseEvent e) {
        touched = null;
        painter.repaint();
    }
}
