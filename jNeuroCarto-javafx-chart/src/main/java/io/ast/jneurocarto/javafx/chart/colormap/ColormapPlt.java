package io.ast.jneurocarto.javafx.chart.colormap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.numpy.NpzFile;
import io.ast.jneurocarto.core.numpy.Numpy;

/**
 * read {@code matplotlib_color_maps.py}
 */
public final class ColormapPlt {

    private static final Logger log = LoggerFactory.getLogger(ColormapPlt.class);
    static final Map<String, LinearColormap> COLORMAPS = new HashMap<>();

    static {
        loadMatplotlibColorMapsPyFile();
    }

    private ColormapPlt() {
        throw new RuntimeException();
    }

    public static void loadMatplotlibColorMapsPyFile() {
        try (var stream = ColormapPlt.class.getResourceAsStream("matplotlib_color_maps.npz")) {
            log.debug("open resource matplotlib_color_maps.npz");
            loadMatplotlibColorMapsPyFile(stream);
        } catch (IOException e) {
            log.warn("loadMatplotlibColorMapsPyFile", e);
        }
    }

    public static void loadMatplotlibColorMapsPyFile(Path file) throws IOException {
        log.debug("load file {}", file);
        try (var fin = Files.newInputStream(file)) {
            loadMatplotlibColorMapsPyFile(fin);
        }
    }

    public static void loadMatplotlibColorMapsPyFile(InputStream in) throws IOException {
        log.debug("load from stream");
        try (var zin = new ZipInputStream(new BufferedInputStream(in))) {
            loadMatplotlibColorMapsPyFile(zin);
        }
    }

    private static void loadMatplotlibColorMapsPyFile(ZipInputStream zin) throws IOException {
        var file = NpzFile.open(zin);
        String name;

        while ((name = file.getNextArray()) != null) {
            var array = file.read(Numpy.ofD2Double()); // double[N][4]
            var stop = new ArrayList<Stop>(array.length);
            for (double[] values : array) {
                stop.add(new Stop(values[0], new Color(values[1], values[2], values[3], 1)));
            }
            var colormap = new LinearColormap(stop);
            COLORMAPS.put(name, colormap);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            loadMatplotlibColorMapsPyFile();
        } else {
            loadMatplotlibColorMapsPyFile(Path.of(args[0]));
        }
        var line = COLORMAPS.keySet().stream().sorted().collect(Collectors.joining(", "));
        System.out.println(line);
    }
}
