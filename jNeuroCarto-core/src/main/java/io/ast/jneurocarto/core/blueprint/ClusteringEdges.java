package io.ast.jneurocarto.core.blueprint;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ClusteringEdges(int category, int shank, List<Corner> edges) {

    /// corner code:
    /// ```text
    /// 3 2 1
    /// 4 8 0
    /// 5 6 7
    ///```
    ///
    /// @param x      bottom left x position in um.
    /// @param y      bottom left y position in um.
    /// @param corner corner code
    public record Corner(int x, int y, int corner) {
    }

    public int[] x() {
        return edges.stream().mapToInt(Corner::x).toArray();
    }

    public int[] y() {
        return edges.stream().mapToInt(Corner::y).toArray();
    }

    public ClusteringEdges withShank(int shank) {
        return new ClusteringEdges(category, shank, edges);
    }

    public ClusteringEdges withCategory(int category) {
        return new ClusteringEdges(category, shank, edges);
    }

    public ClusteringEdges setCorner(int dx, int dy) {
        if (dx < 0 || dy < 0) throw new IllegalArgumentException();
        var dx2 = new int[]{0, dx, 0, -dx, 0, -dx, 0, dx, 0};
        var dy2 = new int[]{0, dy, 0, dy, 0, -dy, 0, -dy, 0};
        var edges1 = this.edges.stream().filter(corner -> {
            var c = corner.corner;
            // corner at 0, 2, 4, 6 are removed
            return c == 1 || c == 3 || c == 5 || c == 7;
        }).map(corner -> {
            var c = corner.corner;
            return new Corner(corner.x + dx2[c], corner.y + dy2[c], 8);
        }).toList();
        return new ClusteringEdges(category, shank, edges1);
    }

    public int area() {
        var area = 0;
        for (int i = 0, size = edges.size(); i < size; i++) {
            var p = edges.get(i);
            var q = edges.get((i + 1) % size);
            area += (p.x * q.y) - (q.x * p.y);
        }
        return area;
    }

    public ClusteringEdges convex() {
        if (edges.isEmpty()) return this;

        var x1 = edges.stream().mapToInt(Corner::x).min().getAsInt();
        var x2 = edges.stream().mapToInt(Corner::x).max().getAsInt();
        var y1 = edges.stream().mapToInt(Corner::y).min().getAsInt();
        var y2 = edges.stream().mapToInt(Corner::y).max().getAsInt();

        return new ClusteringEdges(category, shank, List.of(
          new Corner(x1, y1, 5),
          new Corner(x2, y1, 7),
          new Corner(x2, y2, 1),
          new Corner(x1, y2, 3)
        ));
    }

    public ClusteringEdges offset(int x, int y) {
        var edges = this.edges.stream().map(it -> new Corner(it.x + x, it.y + y, it.corner)).toList();
        return new ClusteringEdges(category, shank, edges);
    }

    public boolean contains(int x, int y) {
        var left = 0;
        for (int i = 0, size = edges.size(); i < size; i++) {
            var p = edges.get(i);
            var q = edges.get((i + 1) % size);
            if (p.y > q.y) {
                var t = p;
                p = q;
                q = t;
            }
            if (p.y < y && y <= q.y) {
                var xx = p.x + (double) (q.x - p.x) * (y - p.y) / (q.y - p.y);
                if (x > xx) {
                    left++;
                }
            }
        }
        return left % 2 == 1;
    }


}
