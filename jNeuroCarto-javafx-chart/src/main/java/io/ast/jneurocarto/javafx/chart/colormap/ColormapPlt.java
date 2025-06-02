package io.ast.jneurocarto.javafx.chart.colormap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.scene.paint.Color;
import javafx.scene.paint.Stop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        try (var stream = ColormapPlt.class.getResourceAsStream("matplotlib_color_maps.py")) {
            log.debug("open resource matplotlib_color_maps.py");
            loadMatplotlibColorMapsPyFile(stream);
        } catch (IOException e) {
            log.warn("loadMatplotlibColorMapsPyFile", e);
        }
    }

    public static void loadMatplotlibColorMapsPyFile(Path file) throws IOException {
        log.debug("load file {}", file);
        try (var reader = Files.newBufferedReader(file)) {
            loadMatplotlibColorMapsPyFile(reader);
        }
    }

    public static void loadMatplotlibColorMapsPyFile(InputStream in) throws IOException {
        log.debug("load from stream");
        try (var reader = new BufferedReader(new InputStreamReader(in))) {
            loadMatplotlibColorMapsPyFile(reader);
        }
    }

    private static void loadMatplotlibColorMapsPyFile(BufferedReader reader) throws IOException {
        var split = Pattern.compile(",\\s*");
        var isTripleQuote = false;
        var isInBracket = false;
        var currentName = new ArrayList<String>(2);
        var currentStop = new ArrayList<Stop>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals("\"\"\"")) {
                isTripleQuote = !isTripleQuote;
            } else if (isTripleQuote || line.startsWith("#")) {
            } else if (isInBracket && line.startsWith("]")) {
                isInBracket = false;

                var colormap = new LinearColormap(currentStop);
                for (var name : currentName) {
                    COLORMAPS.put(name, colormap);
                }
                currentName.clear();
                currentStop.clear();
            } else if (!isInBracket && line.endsWith("[")) {
                currentName.clear();
                currentStop.clear();

                for (var name : line.split("\\s*=\\s*\\[?")) {
                    currentName.add(name);
                }

                isInBracket = true;
            } else if (isInBracket) {
                var parts = split.split(line.strip());
                var p = Double.parseDouble(parts[0]);
                var r = Double.parseDouble(parts[1]);
                var g = Double.parseDouble(parts[2]);
                var b = Double.parseDouble(parts[3]);
                currentStop.add(new Stop(p, new Color(r, g, b, 1)));
            }
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
