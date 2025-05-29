package io.ast.jneurocarto.javafx.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.numpy.Numpy;
import io.ast.jneurocarto.core.numpy.NumpyHeader;
import io.ast.jneurocarto.javafx.chart.Application;
import io.ast.jneurocarto.javafx.chart.InteractionXYChart;
import io.ast.jneurocarto.javafx.chart.Normalize;
import io.ast.jneurocarto.javafx.chart.XYMatrix;
import picocli.CommandLine;

@CommandLine.Command(
  name = "matrix",
  usageHelpAutoWidth = true,
  description = "show matrix image"
)
public class Matrix implements Application.ApplicationContent, Runnable {

    @CommandLine.Option(names = {"-h", "-?", "--help"}, usageHelp = true)
    public boolean help;

    @CommandLine.Option(names = "--cmap", defaultValue = "jet",
      description = "colormap")
    String colormap;

    @CommandLine.Option(names = {"-s", "--shape"}, arity = "1", paramLabel = "[[S,]R,]C", required = true,
      converter = IntArrayConverter.class,
      description = "data shape.")
    IntArray dataShape;
    int[] shape;

    @CommandLine.Option(names = {"--space"}, arity = "1", paramLabel = "UM",
      defaultValue = "10,2,2", split = ",",
      description = "electrode space S,R,C.")
    int[] space;

    public record IntArray(int[] shape) {
    }

    public static class IntArrayConverter implements CommandLine.ITypeConverter<IntArray> {
        @Override
        public IntArray convert(String value) {
            var ret = Arrays.stream(value.split(",")).mapToInt(s -> {
                if (s.isEmpty()) {
                    return -1;
                } else {
                    return Integer.parseInt(s);
                }
            }).toArray();

            if (ret.length == 1) {
                ret = new int[]{-1, ret[0]};
            }
            return new IntArray(ret);
        }
    }

    @CommandLine.Option(names = {"-n", "--norm", "--normalize"}, arity = "1", paramLabel = "V,V",
      converter = NormalizeConverter.class,
      description = "data normalize.")
    Normalize normalize;

    public static class NormalizeConverter implements CommandLine.ITypeConverter<Normalize> {
        @Override
        public Normalize convert(String value) {
            var ret = Arrays.stream(value.split(",")).mapToDouble(Double::parseDouble).toArray();

            if (ret.length != 2) {
                throw new IllegalArgumentException();
            }
            return new Normalize(ret[0], ret[1]);
        }
    }

    @CommandLine.Option(names = {"-o", "--order"}, paramLabel = "SRC", defaultValue = "",
      description = "data shape order.")
    String order;
    int[] orderIndex; // {S, R, C}
    int[] length; // {S, R, C}

    @CommandLine.Option(names = "--interpolate",
      description = "interpolate NaN values")
    boolean interpolateNaN;

    @CommandLine.Parameters(index = "0", paramLabel = "FILE",
      description = "npy data file")
    Path dataFile;
    double[] data;

    @CommandLine.ParentCommand
    public Chart parent;

    private Logger log;

    @Override
    public void run() {
        log = LoggerFactory.getLogger(Matrix.class);

        loadData();
        initData();
        processData();

        log.debug("launch");
        parent.launch(new Application(this));
    }

    private void loadData() {
        log.debug("load data");
        shape = dataShape == null ? null : dataShape.shape;
        if (space.length != 3) throw new RuntimeException("wrong space : " + Arrays.toString(shape));

        try {
            var read = Numpy.read(dataFile, header -> {
                var shape = header.shape();
                if (this.shape == null) {
                    this.shape = header.shape();
                } else if (!NumpyHeader.match(this.shape, shape)) {
                    throw new RuntimeException("shape mismatch: " + Arrays.toString(this.shape) + " != data " + Arrays.toString(shape));
                }
                return Numpy.ofFlattenDouble();
            });
            data = ((Numpy.FlattenDoubleArray) read.data()).array();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.debug("shape = {}", Arrays.toString(shape));
        log.debug("data.length = {}", data.length);
    }

    private void initData() {
        log.debug("init data");

        if (shape.length < 2 || shape.length > 3) {
            throw new RuntimeException("not a 2-d or 3-d shape " + Arrays.toString(shape));
        }

        if (order == null || order.isEmpty()) {
            order = switch (shape.length) {
                case 2 -> "RC";
                case 3 -> "SRC";
                default -> throw new RuntimeException();
            };
        }
        log.debug("order = {}", order);

        orderIndex = new int[3];
        length = new int[3];

        orderIndex[0] = -1;
        length[0] = 1;

        switch (shape.length) {
        case 3:
            orderIndex[0] = order.indexOf('S');
            length[0] = shape[orderIndex[0]];
        case 2:
            orderIndex[1] = order.indexOf('R');
            length[1] = shape[orderIndex[1]];
            orderIndex[2] = order.indexOf('C');
            length[2] = shape[orderIndex[2]];
        }

        log.debug("orderIndex = {}", Arrays.toString(orderIndex));
        log.debug("length = {}", Arrays.toString(length));
    }

    private void processData() {
        if (interpolateNaN) {
            log.debug("interpolateNaN over ({},{},{})", shape[orderIndex[0]], shape[orderIndex[1]], shape[orderIndex[2]]);
            if (!Objects.equals(order, "SRC")) log.warn("order not SRC, interpolateNaN may give wrong result.");
            var toolkit = BlueprintToolkit.dummy(shape[orderIndex[0]], shape[orderIndex[1]], shape[orderIndex[2]]);
            data = toolkit.interpolateNaN(data, 3, BlueprintToolkit.InterpolateMethod.mean);
        }
    }

    private int index(int s, int r, int c) {
        var i = new int[3];
        if (orderIndex[0] >= 0) {
            i[orderIndex[0]] = s;
            i[orderIndex[1]] = r;
            i[orderIndex[2]] = c;
            var ret = i[0] * shape[1] * shape[2] + i[1] * shape[2] + i[2];
//            System.out.printf("(s,r,c)=(%d,%d,%d)->%s=%d\n", s, r, c, Arrays.toString(i), ret);
            return ret;
        } else {
            i[orderIndex[1]] = r;
            i[orderIndex[2]] = c;
            assert i[2] == 0;
            var ret = i[0] * shape[1] + i[1];
//            System.out.printf("(r,c)=(%d,%d)->%s=%d\n", r, c, Arrays.toString(i), ret);
            return ret;
        }
    }

    private double data(int s, int r, int c) {
        var i = index(s, r, c);
        var ret = data[i];
//        if (!Double.isNaN(ret)) System.out.printf("data[%d,%d,%d=%d]=%f\n", s, r, c, i, ret);
        return ret;
    }

    /*=============*
     * Application *
     *=============*/

    @Override
    public void setup(InteractionXYChart chart) {
        log.debug("setup");

        var painter = chart.getPlotting();

        var matrix = new XYMatrix[length[0]];
        for (int s = 0; s < length[0]; s++) {
            var builder = painter.imshow()
              .colormap(colormap)
              .extent(s * space[0], 0, length[2], space[2], length[1], space[1]);

            var m = builder.graphics();
            matrix[s] = m;
            log.debug("matric x={}, y={}, w={}, h={}, nx={}, ny={}", m.x(), m.y(), m.w(), m.h(), m.nx(), m.ny());

            for (int r = 0; r < length[1]; r++) {
                for (int c = 0; c < length[2]; c++) {
                    var v = data(s, r, c);
                    if (!Double.isNaN(v)) {
                        builder.addData(c, r, v);
                    }
                }
            }
        }

        if (normalize == null) {
            var norm = XYMatrix.renormalize(matrix, new Normalize(0, 0));
            log.debug("norm = {}", norm);
        } else {
            XYMatrix.renormalize(matrix, normalize);
        }

        var x1 = space[0] * length[0];
        var y1 = space[1] * length[1];
        chart.setResetAxesBoundaries(0, x1, 0, y1);
        log.debug("bound = {}", chart.getResetAxesBoundaries());
        chart.resetAxesBoundaries();
        painter.repaint();
    }
}
