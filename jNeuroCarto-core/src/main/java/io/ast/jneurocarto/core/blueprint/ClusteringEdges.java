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
        return setCorner(dx, dy, -dx, dy, -dx, -dy, dx, -dy);
    }

    public ClusteringEdges setCorner(int dx1, int dy1, int dx3, int dy3, int dx5, int dy5, int dx7, int dy7) {
        var dx = new int[]{0, dx1, 0, dx3, 0, dx5, 0, dx7, 0};
        var dy = new int[]{0, dy1, 0, dy3, 0, dy5, 0, dy7, 0};
        var edges = this.edges.stream().filter(corner -> {
            var c = corner.corner;
            // corner at 0, 2, 4, 6 are removed
            return c == 1 || c == 3 || c == 5 || c == 7;
        }).map(corner -> {
            var c = corner.corner;
            return new Corner(corner.x + dx[c], corner.y + dy[c], 8);
        }).toList();
        return new ClusteringEdges(category, shank, edges);
    }

    public ClusteringEdges offset(int x, int y) {
        var edges = this.edges.stream().map(it -> new Corner(it.x + x, it.y + y, it.corner)).toList();
        return new ClusteringEdges(category, shank, edges);
    }


}
