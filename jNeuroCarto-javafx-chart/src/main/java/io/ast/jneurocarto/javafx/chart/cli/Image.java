package io.ast.jneurocarto.javafx.chart.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.numpy.FlatIntArray;
import io.ast.jneurocarto.core.numpy.Numpy;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.data.XYMatrix;
import io.ast.jneurocarto.javafx.chart.event.ChartMouseEvent;
import picocli.CommandLine;

@CommandLine.Command(
    name = "image",
    usageHelpAutoWidth = true,
    description = "show image matrix"
)
public class Image implements Main.Content, Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--cmap", defaultValue = "jet",
        description = "colormap")
    String colormap;

    @CommandLine.Parameters(index = "0", paramLabel = "FILE",
        description = "npy data file")
    Path dataFile;

    @CommandLine.ParentCommand
    public Main parent;

    private FlatIntArray data;
    private Logger log;


    @Override
    public void run() {
        log = LoggerFactory.getLogger(Image.class);

        loadData();
        processData();

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

        var shape = data.shape();
        var toolkit = BlueprintToolkit.dummy(1, shape[0], shape[1]);
        toolkit.from(data.array());

        var i = 1;
        while (toolkit.count(i) > 0) {
            i++;
        }
        var ii = i;
        System.out.println("i = " + i);

        for (var c : HPF) {
            toolkit.set(ii, e -> e.c() == c);
        }
        toolkit.unset(e -> e.c() != ii);
        toolkit.apply(data.array());
    }


    /*=============*
     * Application *
     *=============*/

    private XYMatrix matrix;

    @Override
    public void setup(InteractionXYChart chart) {
        log.debug("setup");

        var painter = chart.getPlotting();
        matrix = painter.imshow(data)
            .colormap(colormap)
            .normalize(0, 1)
            .graphics();
        System.out.println(matrix.nx() + ", " + matrix.ny());

        chart.addEventHandler(ChartMouseEvent.CHART_MOUSE_PRESSED, this::onMousePressed);
        painter.repaint();
    }

    private void onMousePressed(ChartMouseEvent e) {
        var xy = matrix.touch(e.point);
        if (xy != null) {
            System.out.println("x=" + xy.x() + ",y=" + xy.y() + ",v=" + xy.v());
        }
    }
}
