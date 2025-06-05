package io.ast.jneurocarto.core.blueprint;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Gatherer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
    public record Corner(double x, double y, int corner) {
    }

    public double x0() {
        return edges.stream().mapToDouble(Corner::x).min().orElse(Double.NaN);
    }

    public double y0() {
        return edges.stream().mapToDouble(Corner::y).min().orElse(Double.NaN);
    }

    public double[] x() {
        return edges.stream().mapToDouble(Corner::x).toArray();
    }

    public double[] y() {
        return edges.stream().mapToDouble(Corner::y).toArray();
    }

    public ClusteringEdges withShank(int shank) {
        return new ClusteringEdges(category, shank, edges);
    }

    public ClusteringEdges withCategory(int category) {
        return new ClusteringEdges(category, shank, edges);
    }

    public ClusteringEdges setCorner(double dx, double dy) {
        if (dx < 0 || dy < 0) throw new IllegalArgumentException();
        var dxx = new double[]{0, dx, 0, -dx, 0, -dx, 0, dx, 0};
        var dyy = new double[]{0, dy, 0, dy, 0, -dy, 0, -dy, 0};
        var edges = this.edges.stream().filter(corner -> {
            var c = corner.corner;
            // corner at 0, 2, 4, 6 are removed
            return c == 1 || c == 3 || c == 5 || c == 7 || c == 8;
        }).map(corner -> {
            var c = corner.corner;
            return new Corner(corner.x + dxx[c], corner.y + dyy[c], 8);
        }).toList();
        return new ClusteringEdges(category, shank, edges);
    }

    private Corner setCorner(Corner corner, double dx, double dy) {
        var x = corner.x;
        var y = corner.y;
        return switch (corner.corner) {
            case 0 -> new Corner(x + dx, y + 0, 8);
            case 1 -> new Corner(x + dx, y + dy, 8);
            case 2 -> new Corner(x + 0, y + dy, 8);
            case 3 -> new Corner(x - dx, y + dy, 8);
            case 4 -> new Corner(x - dx, y + 0, 8);
            case 5 -> new Corner(x - dx, y - dy, 8);
            case 6 -> new Corner(x + 0, y - dy, 8);
            case 7 -> new Corner(x + dx, y - dy, 8);
            case 8 -> corner;
            default -> throw new IllegalArgumentException();
        };
    }

    public ClusteringEdges smallCornerRemoving(double dx, double dy) {
        if (dx < 0 || dy < 0) throw new IllegalArgumentException();
        var edges = this.edges.stream().gather(Gatherer.<Corner, @Nullable Corner[], Corner>ofSequential(
            () -> new Corner[3], // [0, n-2, n-1]
            (state, element, downstream) -> {
                if (state[0] == null) {
                    state[0] = element;
                    state[1] = element;
                    return !downstream.isRejecting();
                } else if (state[2] == null) {
                    state[2] = element;
                    return !downstream.isRejecting();
                } else if (smallCornerRemovingPushing(state[1], state[2], element, dx, dy)) {
                    var push = state[1];
                    state[1] = state[2];
                    state[2] = element;
                    return downstream.push(push);
                } else {
                    state[2] = element;
                    return !downstream.isRejecting();
                }
            },
            (state, downstream) -> {
                if (state[0] != null) {
                    assert state[1] != null;
                    if (state[2] == null) {
                        downstream.push(state[0]);
                    } else if (smallCornerRemovingPushing(state[1], state[2], state[0], dx, dy)) {
                        downstream.push(state[1]);
                        downstream.push(state[2]);
                    } else {
                        downstream.push(state[1]);
                    }
                }
            }
        )).toList();
        return new ClusteringEdges(category, shank, edges);
    }

    private boolean smallCornerRemovingPushing(Corner e0, Corner e1, Corner e2, double dx, double dy) {
        var x0 = e0.x;
        var y0 = e0.y;
//        var c0 = e0.corner;
        var x1 = e1.x;
        var y1 = e1.y;
        var c1 = e1.corner;
        var x2 = e2.x;
        var y2 = e2.y;
//        var c2 = e2.corner;

        if (x0 == x1 && x1 == x2) {
            return false;
        } else if (y0 == y1 && y1 == y2) {
            return false;
        } else if (x0 == x1 && y1 == y2 && Math.abs(y1 - y0) <= dy && Math.abs(x2 - x1) <= dx) {
            if (y0 < y1 && (c1 == 5 || c1 == 7)) {
                return false;
            } else if (y0 > y1 && (c1 == 1 || c1 == 3)) {
                return false;
            }
        } else if (y0 == y1 && x1 == x2 && Math.abs(x1 - x0) <= dx && Math.abs(y2 - y1) <= dy) {
            if (x0 < x1 && (c1 == 3 || c1 == 5)) {
                return false;
            } else if (x0 > x1 && (c1 == 1 || c1 == 7)) {
                return false;
            }
        }

        return true;
    }

    public double area() {
        var area = 0.0;
        for (int i = 0, size = edges.size(); i < size; i++) {
            var p = edges.get(i);
            var q = edges.get((i + 1) % size);

            var px = p.x;
            var py = p.y;
            var qy = q.y;
            var qx = q.x;

            area += (px * qy) - (qx * py);
        }
        return area / 2;
    }

    public ClusteringEdges convex() {
        if (edges.isEmpty()) return this;

        var x1 = edges.stream().mapToDouble(Corner::x).min().getAsDouble();
        var x2 = edges.stream().mapToDouble(Corner::x).max().getAsDouble();
        var y1 = edges.stream().mapToDouble(Corner::y).min().getAsDouble();
        var y2 = edges.stream().mapToDouble(Corner::y).max().getAsDouble();

        return new ClusteringEdges(category, shank, List.of(
            new Corner(x1, y1, 5),
            new Corner(x2, y1, 7),
            new Corner(x2, y2, 1),
            new Corner(x1, y2, 3)
        ));
    }

    public ClusteringEdges offset(double x, double y) {
        var edges = this.edges.stream().map(it -> new Corner(it.x + x, it.y + y, it.corner)).toList();
        return new ClusteringEdges(category, shank, edges);
    }

    public ClusteringEdges transform(double mxx, double mxy, double mtx,
                                     double myx, double myy, double mty) {
        var edges = this.edges.stream().map(it -> {
            var x = it.x * mxx + it.y * mxy + mtx;
            var y = it.x * myx + it.y * myy + mty;
            return new Corner(x, y, it.corner);
        }).toList();
        return new ClusteringEdges(category, shank, edges);
    }

    public ClusteringEdges map(Function<Corner, Corner> mapper) {
        var edges = this.edges.stream().map(mapper).toList();
        return new ClusteringEdges(category, shank, edges);
    }

    public boolean contains(double x, double y) {
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
                var xx = p.x + (q.x - p.x) * (y - p.y) / (q.y - p.y);
                if (x > xx) {
                    left++;
                }
            }
        }
        return left % 2 == 1;
    }


}
