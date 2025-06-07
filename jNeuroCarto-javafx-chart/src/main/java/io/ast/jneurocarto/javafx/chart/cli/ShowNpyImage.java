package io.ast.jneurocarto.javafx.chart.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.blueprint.ClusteringEdges;
import io.ast.jneurocarto.core.numpy.FlatIntArray;
import io.ast.jneurocarto.core.numpy.Numpy;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;
import io.ast.jneurocarto.javafx.chart.data.XYMatrix;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import picocli.CommandLine;

@CommandLine.Command(
    name = "npy-image",
    sortOptions = false,
    usageHelpAutoWidth = true,
    mixinStandardHelpOptions = true,
    description = "show numpy image matrix"
)
public class ShowNpyImage implements Main.Content, Runnable {

    @CommandLine.Option(names = "--cmap", defaultValue = "jet",
        description = "colormap")
    String colormap;

    @CommandLine.Option(names = "--mask")
    boolean processMasking;

    @CommandLine.Option(names = "--edge")
    boolean processEdge;

    @CommandLine.Parameters(index = "0", paramLabel = "FILE",
        description = "npy data file")
    Path dataFile;

    @CommandLine.ParentCommand
    public Main parent;

    private FlatIntArray data;
    private Logger log;


    @Override
    public void run() {
        log = LoggerFactory.getLogger(ShowNpyImage.class);

        loadData();
        System.out.println("shape = " + Arrays.toString(data.shape()));
        if (processMasking) processData();

        log.debug("launch");
        parent.launch(this);
    }

    private void loadData() {
        log.debug("load data");

        try {
            data = Numpy.read(dataFile, Numpy.ofFlattenInt());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("shape = {}", Arrays.toString(data.shape()));
    }

    private void processData() {
        var HPF = new int[]{
            484682470, 139, 843, 909, 1037, 526, 463, 10703, 10704, 19, 20, 726, 982, 918, 727, 664, 28, 926, 543, 1121,
            423, 743, 484682508, 52, 822, 502, 375, 1080, 632, 1084, 589508447, 382
        };

        var toolkit = BlueprintToolkit.dummy(data);

        for (var c : HPF) {
            toolkit.set(1, e -> e.c() == c);
        }
        toolkit.set(0, e -> e.c() != 1);
        toolkit.from(toolkit.findClustering(1, true).clustering());
        toolkit.apply(data);
    }

    private void processEdge() {
        var toolkit = BlueprintToolkit.dummy(data);

        var i = 0;
        for (ClusteringEdges clustering : toolkit.getClusteringEdges()) {
            var builder = painter.lines();
            switch (i) {
            case 0 -> builder.line(Color.WHITE);
            case 1 -> builder.line(Color.LIGHTGRAY);
            case 2 -> builder.line(Color.GRAY);
            case 3 -> builder.line(Color.DARKGRAY);
            case 4 -> builder.line(Color.YELLOW);
            case 5 -> builder.line(Color.ORANGE);
            case 6 -> builder.line(Color.GREEN);
            case 7 -> builder.line(Color.CYAN);
            default -> builder.line(Color.BLACK);
            }

            clustering/*.smallCornerRemoving(1, 1)*/.edges().forEach(it -> {
                builder.addPoint(it.x() + 0.5, it.y() + 0.5);
            });
            i++;
        }

        painter.repaint();
    }


    /*=============*
     * Application *
     *=============*/

    private InteractionXYPainter painter;
    private XYMatrix matrix;

    @Override
    public void setup(InteractionXYChart chart) {
        log.debug("setup");

        var shape = data.shape();
        var row = shape[0];
        var col = shape[1];

        painter = chart.getPlotting();
        matrix = painter.imshow(data)
            .colormap(colormap)
            .normalize(0, processMasking ? 10 : 1000)
//            .extent(2, 2, col + 1, row + 1)
            .graphics();

        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_PRESSED, this::onMousePressed);
        painter.repaint();

        if (processEdge) processEdge();
    }

    private void onMousePressed(ChartMouseEvent e) {
        var xy = matrix.touch(e.point);
        if (xy != null) {
            System.out.println("x=" + xy.x() + ",y=" + xy.y() + ",v=" + xy.v());
        }
    }
}
