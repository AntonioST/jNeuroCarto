package io.ast.jneurocarto.javafx.chart.cli;

import java.io.IOException;
import java.nio.file.Path;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.data.XYImage;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseDraggingHandler;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import picocli.CommandLine;

@CommandLine.Command(
    name = "image",
    sortOptions = false,
    usageHelpAutoWidth = true,
    description = "show image"
)
public class ShowImage implements Main.Content, Runnable, ChartMouseDraggingHandler {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Parameters(index = "0", paramLabel = "FILE",
        description = "image file")
    Path file;

    @CommandLine.ParentCommand
    public Main parent;

    private Logger log;


    @Override
    public void run() {
        log = LoggerFactory.getLogger(ShowImage.class);

        log.debug("launch");
        parent.launch(this);
    }

    /*=============*
     * Application *
     *=============*/

    private InteractionXYPainter painter;
    private XYImage image;
    private boolean onTranslate;
    private boolean onRotate;
    private boolean onScale;

    @Override
    public void setup(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::onKeyAction);
    }

    @Override
    public void setup(InteractionXYChart chart) {
        log.debug("setup");

        painter = chart.getPlotting();
        var builder = painter.newImageLayer();

        try {
            image = builder.addImage(file);
        } catch (IOException e) {
            log.error("addImage", e);
            return;
        }

        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_CLICKED, this::onMouseClick);
        chart.addEventFilter(ScrollEvent.SCROLL, this::onMouseScall);
        ChartMouseDraggingHandler.setupChartMouseDraggingHandler(chart, this);

        painter.repaint();
    }


    private void onKeyAction(KeyEvent e) {
        if (e.getCode() == KeyCode.R) {
            onRotate = !onRotate;
            onScale = false;
            onTranslate = false;
            System.out.println("onRotate = " + onRotate);
        } else if (e.getCode() == KeyCode.S) {
            onScale = !onScale;
            onRotate = false;
            onTranslate = false;
            System.out.println("onScale = " + onScale);
        } else if (e.getCode() == KeyCode.T) {
            onTranslate = !onTranslate;
            onRotate = false;
            onScale = false;
            System.out.println("onTranslate = " + onTranslate);
        } else if (e.getCode() == KeyCode.Q) {
            onTranslate = false;
            onRotate = false;
            onScale = false;
            System.out.println("clear transform");
        }
    }

    private void onMouseClick(ChartMouseEvent e) {
        if (onTranslate && e.getButton() == MouseButton.PRIMARY) {
            image.moveTo(e.point);
            e.consume();
            painter.repaint();
        }
    }

    @Override
    public boolean onChartMouseDragDetect(ChartMouseEvent e) {
        return onTranslate && e.getButton() == MouseButton.PRIMARY;
    }

    @Override
    public void onChartMouseDragging(ChartMouseEvent p, ChartMouseEvent e) {
        image.move(e.delta(p));
        painter.repaint();
    }

    @Override
    public void onChartMouseDragDone(ChartMouseEvent e) {
    }

    private void onMouseScall(ScrollEvent e) {
        var delta = e.getDeltaY();
        if (Math.abs(delta) > 1) {
            if (onRotate) {
                if (delta > 0) {
                    image.r += 5;
                } else {
                    image.r -= 5;
                }
                e.consume();
            }

            if (onScale) {
                if (delta > 0) {
                    image.scale(Math.min(3, image.sx + 0.1));
                } else {
                    image.scale(Math.max(0.1, image.sx - 0.1));
                }
                e.consume();
            }

            if (e.isConsumed()) {
                painter.repaint();
            }
        }
    }
}
