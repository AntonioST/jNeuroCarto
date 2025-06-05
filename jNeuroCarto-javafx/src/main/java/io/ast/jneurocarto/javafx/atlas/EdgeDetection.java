package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import javafx.geometry.Point2D;

import io.ast.jneurocarto.atlas.ImageArray;

public final class EdgeDetection {
    private static final int[] dx = {-1, 0, 1, -1, 1, -1, 0, 1};
    private static final int[] dy = {-1, -1, -1, 0, 0, 1, 1, 1};

    private EdgeDetection() {
        throw new RuntimeException();
    }

    public static List<Point2D> detect(ImageArray image, int value) {
        return detect(image, new int[]{value});
    }

    public static List<Point2D> detect(ImageArray image, int[] value) {
        value = value.clone();
        Arrays.sort(value);

        var array = image.image();
        var visited = new BitSet(array.length);
        var ret = new ArrayList<Point2D>();

        for (int y = 0; y < image.h(); y++) {
            for (int x = 0; x < image.w(); x++) {
                var i = image.index(x, y);
                if (isin(value, array[i]) && !visited.get(i)) {
                    fill(image, visited, x, y, value, ret);
                }
            }
        }

        return ret;
    }

    // TODO bad ordering
    private static void fill(ImageArray image, BitSet visited, int x, int y, int[] value, ArrayList<Point2D> ret) {
        var added = false;

        var w = image.w();
        var array = image.image();
        var queue = new ArrayList<Integer>();
        var i = image.index(x, y);
        queue.add(i);
        visited.set(i);

        while (!queue.isEmpty()) {
            var j = queue.removeFirst();
            var px = j % w;
            var py = j / w;
            var isBoundary = false;

            for (int d = 0; d < 8; d++) {
                var qx = px + dx[d];
                var qy = py + dy[d];
                if (image.inBoundary(qx, qy)) {
                    var k = image.index(qx, qy);
                    if (isin(value, array[k])) {
                        if (!visited.get(k)) {
                            visited.set(k);
                            queue.add(k);
                        }
                    } else {
                        isBoundary = true;
                    }
                } else {
                    isBoundary = true;
                }
            }
            if (isBoundary) {
                if (!added && !ret.isEmpty()) ret.add(new Point2D(Double.NaN, Double.NaN));
                added = true;
                ret.add(new Point2D(px, py));
            }
        }
    }

    private static boolean isin(int[] array, int value) {
        return Arrays.binarySearch(array, value) >= 0;
    }
}
