package io.ast.jneurocarto.core.blueprint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public final class BlueprintToolkit<T> {

    /**
     * wrapped blueprint.
     */
    private final Blueprint<T> blueprint;

    /**
     * warp {@link BlueprintToolkit} onto {@code blueprint}.
     *
     * @param blueprint
     */
    public BlueprintToolkit(Blueprint<T> blueprint) {
        this.blueprint = blueprint;
    }

    /**
     * create a copied {@link BlueprintToolkit} from {@code blueprint}.
     * The wrapped {@link Blueprint} will be cloned.
     *
     * @param blueprint
     */
    public BlueprintToolkit(BlueprintToolkit<T> blueprint) {
        this(new Blueprint<>(blueprint.blueprint));
    }

    /*========*
     * getter *
     *========*/

    public int length() {
        return blueprint.shank.length;
    }

    public int[] shank() {
        return blueprint.shank;
    }

    public int nShank() {
        return (int) Arrays.stream(shank()).distinct().count();
    }

    public int[] posx() {
        return blueprint.posx;
    }

    public int[] posy() {
        return blueprint.posy;
    }

    public int dx() {
        return blueprint.dx;
    }

    public int dy() {
        return blueprint.dy;
    }

    /*=================*
     * blueprint array *
     *=================*/

    public int[] blueprint() {
        return blueprint.blueprint;
    }

    public int[] newBlueprint() {
        var ret = new int[length()];
        Arrays.fill(ret, ProbeDescription.CATE_UNSET);
        return ret;
    }

    public void setBlueprint(int[] blueprint) {
        var dst = this.blueprint.blueprint;
        if (blueprint.length != dst.length) throw new RuntimeException();
        if (dst != blueprint) {
            System.arraycopy(blueprint, 0, dst, 0, dst.length);
            markDirty();
        }
    }

    public void setBlueprint(Blueprint<T> blueprint) {
        if (blueprint != this.blueprint) {
            setBlueprint(blueprint.blueprint);
        }
    }

    public void setBlueprint(BlueprintToolkit<T> blueprint) {
        setBlueprint(blueprint.blueprint);
    }

    public void mergeBlueprint(int[] blueprint) {
        var dst = this.blueprint.blueprint;
        if (blueprint.length != dst.length) throw new RuntimeException();
        if (dst != blueprint) {
            for (int i = 0, length = dst.length; i < length; i++) {
                if (dst[i] == ProbeDescription.CATE_UNSET) dst[i] = blueprint[i];
            }

            markDirty();
        }
    }

    public void mergeBlueprint(Blueprint<T> blueprint) {
        if (blueprint != this.blueprint) {
            mergeBlueprint(blueprint.blueprint);
        }
    }

    public void mergeBlueprint(BlueprintToolkit<T> blueprint) {
        mergeBlueprint(blueprint.blueprint);
    }

    public void markDirty() {
        blueprint.modified = true;
    }

    public void printBlueprint() {
        printBlueprint(blueprint.blueprint);
    }

    public String toStringBlueprint() {
        return toStringBlueprint(blueprint.blueprint);
    }

    public void printBlueprint(int[] blueprint) {
        try {
            printBlueprint(blueprint, System.out);
        } catch (IOException e) {
        }
    }

    public String toStringBlueprint(int[] blueprint) {
        var sb = new StringBuilder();
        try {
            printBlueprint(blueprint, sb);
        } catch (IOException e) {
        }
        return sb.toString();
    }

    public void printBlueprint(int[] blueprint, Appendable out) throws IOException {
        if (length() != blueprint.length) throw new RuntimeException();

        var shanks = Arrays.stream(shank()).distinct().sorted().toArray();
        var posx = Arrays.stream(posx()).distinct().sorted().toArray();
        var posy = Arrays.stream(posy()).distinct().sorted().toArray();
        var maxCate = Arrays.stream(blueprint).max().orElse(0);
        var format = maxCate >= 10 ? "%2d" : "%d";
        var empty = maxCate >= 10 ? "  " : " ";

        for (int si = 0, ns = shanks.length; si < ns; si++) {
            int s = shanks[si];
            if (si > 0) out.append("-".repeat(posx.length * 2 - 1)).append('\n');
            for (int yi = 0, ny = posy.length; yi < ny; yi++) {
                int y = posy[yi];
                for (int xi = 0, nx = posx.length; xi < nx; xi++) {
                    int x = posx[xi];
                    int i = index(s, x, y);
                    if (xi > 0) out.append(' ');
                    if (i >= 0) {
                        out.append(format.formatted(blueprint[i]));
                    } else {
                        out.append(empty);
                    }
                }
                out.append('\n');
            }
        }
    }

    /*===========*
     * utilities *
     *===========*/

    public int index(int s, int x, int y) {
        var shank = shank();
        var posx = posx();
        var posy = posy();
        for (int i = 0, length = length(); i < length; i++) {
            // posy has move unique value in general, so we test first for shortcut fail earlier.
            if (posy[i] == y && shank[i] == s && posx[i] == x) return i;
        }
        return -1;
    }

    private int[] and(int[] a, int[] b, int t, int @Nullable [] c) {
        int length = a.length;
        if (b.length != length) throw new RuntimeException();

        if (c == null) {
            c = new int[length];
        } else if (c.length != length) {
            throw new RuntimeException();
        }

        for (int i = 0; i < length; i++) {
            c[i] = a[i] * b[i] != 0 ? t : 0;
        }
        return c;
    }

    private int[] or(int[] a, int[] b, int t, int @Nullable [] c) {
        int length = a.length;
        if (b.length != length) throw new RuntimeException();

        if (c == null) {
            c = new int[length];
        } else if (c.length != length) {
            throw new RuntimeException();
        }

        for (int i = 0; i < length; i++) {
            c[i] = a[i] + b[i] != 0 ? t : 0;
        }
        return c;
    }

    private int[] merge(int[] a, int[] b, int @Nullable [] c) {
        int length = a.length;
        if (b.length != length) throw new RuntimeException();

        if (c == null) {
            c = new int[length];
        } else if (c.length != length) {
            throw new RuntimeException();
        }

        for (int i = 0; i < length; i++) {
            c[i] = a[i] != 0 ? a[i] : b[i];
        }
        return c;
    }


    /*============================*
     * category zone manipulation *
     *============================*/

    /**
     * @param step y movement
     */
    public void move(int step) {
        if (step == 0 || length() == 0) return;
        setBlueprint(move(blueprint(), step));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @return moved result, a copied {@code blueprint}.
     */
    public int[] move(int[] blueprint, int step) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = new int[length];
        if (step == 0 || length() == 0) return ret;

        return move(ret, blueprint, step, (_, _) -> 1);
    }


    /**
     * @param step     y movement
     * @param category restrict on category
     */
    public void move(int step, int category) {
        if (step == 0 || length() == 0) return;
        setBlueprint(move(blueprint(), step, category));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param category  restrict on category
     * @return moved result, a copied {@code blueprint}.
     */
    public int[] move(int[] blueprint, int step, int category) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = new int[length];
        if (step == 0 || length() == 0) return ret;

        return move(ret, blueprint, step, (_, cate) -> cate == category ? 1 : 0);
    }

    /**
     * @param step  y movement
     * @param index electrode indexes
     */
    public void move(int step, int[] index) {
        if (step == 0 || length() == 0 || index.length == 0) return;
        setBlueprint(move(blueprint(), step, index));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param index     electrode indexes
     * @return moved result, a copied {@code blueprint}.
     */
    public int[] move(int[] blueprint, int step, int[] index) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = new int[length];
        if (step == 0 || length == 0 || index.length == 0) return ret;

        Arrays.sort(index);
        var pointer = new AtomicInteger();

        return move(ret, blueprint, step, (i, _) -> {
            var p = pointer.get();
            if (p < index.length && index[p] == i) {
                return pointer.incrementAndGet();
            } else {
                return 0;
            }
        });
    }

    /**
     * @param step y movement
     * @param mask electrode mask array
     */
    public void move(int step, boolean[] mask) {
        if (length() != mask.length) throw new IllegalArgumentException();
        setBlueprint(move(blueprint(), step, mask));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param mask      electrode mask array
     * @return moved result, a copied {@code blueprint}.
     */
    public int[] move(int[] blueprint, int step, boolean[] mask) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (length != mask.length) throw new RuntimeException();

        var ret = new int[length];
        if (step == 0 || length == 0) return ret;

        var x = ProbeDescription.CATE_UNSET;
        return move(ret, blueprint, step, (i, cate) -> (cate != x && mask[i]) ? 1 : 0);
    }

    /**
     * move electrodes along y-axis.
     * <br/>
     * This method does not reset the content of {@code output}.
     *
     * @param output    output blueprint.
     * @param blueprint source blueprint.
     * @param step      y movement
     * @param tester    whether move this electrode with the signature {@code (electrode, category) -> 1_or_0}.
     * @return {@code output} itself.
     */
    public int[] move(int[] output, int[] blueprint, int step, IntBinaryOperator tester) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (length != output.length) throw new RuntimeException();
        if (step == 0 || length == 0) return output;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        for (int i = 0; i < length; i++) {
            int j = i;
            var cate = blueprint[i];

            if (tester.applyAsInt(i, cate) > 0) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i] + step * dy;
                j = index(s, x, y);
            }

            if (j >= 0 && output[j] == ProbeDescription.CATE_UNSET) {
                output[j] = cate;
            }
        }

        return output;
    }

    /*=======================*
     * electrode surrounding *
     *=======================*/

    /// get surrounding electrode index of the {@code electrode}.
    ///
    /// corner code:
    /// ```text
    /// 3 2 1
    /// 4 8 0
    /// 5 6 7
    ///```
    ///
    /// @param electrode electrode index
    /// @param diagonal
    /// @return 8-length electrode index array. {@code -1} if no electrode.
    public int[] surrounding(int electrode, boolean diagonal) {
        var ret = new int[8];
        surrounding(electrode, diagonal, ret);
        return ret;
    }

    private void surrounding(int electrode, boolean diagonal, int[] output) {
        assert output.length == 8;
        var s = shank()[electrode];
        var x = posx()[electrode];
        var y = posy()[electrode];

        if (diagonal) {
            for (int i = 0; i < 8; i++) {
                output[i] = surrounding(s, x, y, i);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                // 0, 2, 4, 6
                output[i] = surrounding(s, x, y, i * 2);
                // 1, 3, 5, 7
                output[i + 1] = -1;
            }
        }
    }

    /// get electrode index at corner of the {@code electrode}.
    ///
    /// corner code:
    /// ```text
    /// 3 2 1
    /// 4 8 0
    /// 5 6 7
    ///```
    ///
    /// @param electrode electrode index
    /// @param c         corner code
    /// @return electrode index
    public int surrounding(int electrode, int c) {
        var s = shank()[electrode];
        var x = posx()[electrode];
        var y = posy()[electrode];
        return surrounding(s, x, y, c);
    }

    /// get electrode index at corner of the electrode on (s, x, y).
    ///
    /// corner code:
    /// ```text
    /// 3 2 1
    /// 4 8 0
    /// 5 6 7
    ///```
    ///
    /// @param s shank
    /// @param x x position
    /// @param y y position
    /// @param c corner code
    /// @return electrode index
    public int surrounding(int s, int x, int y, int c) {
        return switch (c % 8) {
            case 0 -> index(s, x + 1, y);
            case 1, -7 -> index(s, x + 1, y + 1);
            case 2, -6 -> index(s, x + 0, y + 1);
            case 3, -5 -> index(s, x - 1, y + 1);
            case 4, -4 -> index(s, x - 1, y + 0);
            case 5, -3 -> index(s, x - 1, y - 1);
            case 6, -2 -> index(s, x + 0, y - 1);
            case 7, -1 -> index(s, x + 1, y - 1);
            default -> throw new IllegalArgumentException();
        };
    }

    /*=====================*
     * category clustering *
     *=====================*/

    /**
     * @param diagonal does surrounding includes electrodes on diagonal?
     * @return {@code E}-length int-array that the surrounding electrode shared same positive int value.
     */
    public Clustering findClustering(boolean diagonal) {
        return findClustering(blueprint(), diagonal);
    }

    public Clustering findClustering(int[] blueprint, boolean diagonal) {
        var length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        var minCate = Arrays.stream(blueprint).min().orElse(ProbeDescription.CATE_UNSET);
        var maxCate = Arrays.stream(blueprint).max().orElse(ProbeDescription.CATE_UNSET);

        var ret = new Clustering(length);

        // electrodes are belonging to same the category.
        if (minCate == maxCate) {
            if (minCate != ProbeDescription.CATE_UNSET) {
                ret.fill(1);
            }
            return ret;
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0; i < length; i++) {
            int cate = blueprint[i];
            if (cate != ProbeDescription.CATE_UNSET) {
                ret.set(i, group++);

                surrounding(i, diagonal, surr);
                for (int k = 0; k < 8; k++) {
                    var j = surr[k];
                    if (j >= 0) {
                        ret.unionClusteringGroup(i, j);
                    }
                }
            }
        }

        return ret;
    }

    public Clustering findClustering(int category, boolean diagonal) {
        return findClustering(blueprint(), category, diagonal);
    }

    public Clustering findClustering(int[] blueprint, int category, boolean diagonal) {
        int length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        var n = (int) Arrays.stream(blueprint).filter(i -> i == category).count();

        var ret = new Clustering(length);

        // electrodes are belonging to same the category.
        if (n == length) {
            ret.fill(1);
            return ret;
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0; i < length; i++) {
            int cate = blueprint[i];
            if (cate == category) {
                ret.set(i, group++);

                surrounding(i, diagonal, surr);
                for (int k = 0; k < 8; k++) {
                    var j = surr[k];
                    if (j >= 0) {
                        ret.unionClusteringGroup(i, j);
                    }
                }
            }
        }

        return ret;
    }


    public List<ClusteringEdges> getClusteringEdges() {
        if (length() == 0) return List.of();
        return getClusteringEdges(findClustering(false));
    }

    public List<ClusteringEdges> getClusteringEdges(int[] blueprint) {
        if (length() != blueprint.length) throw new RuntimeException();
        if (blueprint.length == 0) return List.of();
        return getClusteringEdges(findClustering(blueprint, false));
    }

    public List<ClusteringEdges> getClusteringEdges(int category) {
        if (length() == 0) return List.of();
        return getClusteringEdges(findClustering(category, false));
    }

    public List<ClusteringEdges> getClusteringEdges(int[] blueprint, int category) {
        if (length() != blueprint.length) throw new RuntimeException();
        if (blueprint.length == 0) return List.of();
        return getClusteringEdges(findClustering(blueprint, category, false));
    }

    public List<ClusteringEdges> getClusteringEdges(Clustering clustering) {
        var mode = clustering.modeGroup();
        if (mode.group() == 0) return List.of();

        var src = blueprint();
        var shank = shank();
        var posx = posx();
        var posy = posy();

        var ret = new ArrayList<ClusteringEdges>();

        var index = new int[mode.count()];

        for (var g : clustering.groups()) {
            var size = clustering.indexOfGroup(g, index);
            if (size == 0) continue;

            int s = shank[index[0]];
            int c = src[index[0]];

            if (size == 1) {
                var x = posx[index[0]];
                var y = posy[index[0]];
                ret.add(ClusteringUtils.pointClustering(c, s, x, y));
            } else {
                ret.add(ClusteringUtils.areaClustering(this, c, s, Arrays.copyOfRange(index, 0, size)));
            }
        }

        return ret;
    }

    /*=================*
     * fill clustering *
     *=================*/

    public void fillClusteringEdges(List<ClusteringEdges> edges) {
        var blueprint = blueprint();
        for (var edge : edges) {
            fillClusteringEdges(blueprint, edge);
        }
        markDirty();
    }

    /**
     * @param blueprint output blueprint.
     * @param edges
     * @return
     */
    public int[] fillClusteringEdges(int[] blueprint, List<ClusteringEdges> edges) {
        for (var edge : edges) {
            fillClusteringEdges(blueprint, edge);
        }
        return blueprint;
    }

    public void fillClusteringEdges(List<ClusteringEdges> edges, int category) {
        var clustering = edges.stream().map(it -> it.withCategory(category)).toList();
        fillClusteringEdges(clustering);
    }

    /**
     * @param blueprint output blueprint.
     * @param edges
     * @param category  overwrite category.
     * @return
     */
    public int[] fillClusteringEdges(int[] blueprint, List<ClusteringEdges> edges, int category) {
        var clustering = edges.stream().map(it -> it.withCategory(category)).toList();
        return fillClusteringEdges(blueprint, clustering);
    }

    public void fillClusteringEdges(ClusteringEdges edge) {
        fillClusteringEdges(blueprint(), edge);
        markDirty();
    }

    /**
     * @param blueprint output blueprint.
     * @param edge
     * @return {@code blueprint} itself.
     */
    public int[] fillClusteringEdges(int[] blueprint, ClusteringEdges edge) {
        if (length() != blueprint.length) throw new RuntimeException();

        var c = edge.category();
        var s = edge.shank();
        var e = edge.setCorner(dx() / 2, dx() / 2);

        var shank = shank();
        var posx = posx();
        var posy = posy();

        for (int i = 0, length = blueprint.length; i < length; i++) {
            if (shank[i] == s) {
                var x = posx[i];
                var y = posy[i];
                if (e.contains(x, y)) {
                    blueprint[i] = c;
                }
            }
        }
        return blueprint;
    }

    /*====================*
     * fill category zone *
     *====================*/

    public record AreaThreshold(int lower, int upper) implements IntPredicate {
        public static final AreaThreshold ALL = new AreaThreshold(0, Integer.MAX_VALUE);

        public AreaThreshold(int upper) {
            this(0, upper);
        }

        @Override
        public boolean test(int value) {
            return lower <= value && value < upper;
        }
    }

    /**
     * fill category zone as rectangle.
     *
     * @param category
     */
    public void fill(int category) {
        setBlueprint(fill(blueprint(), category, AreaThreshold.ALL));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param blueprint
     * @param category
     * @return filled result, a copied {@code blueprint}.
     */
    public int[] fill(int[] blueprint, int category) {
        return fill(blueprint, category, AreaThreshold.ALL);
    }

    /**
     * fill category zone as rectangle.
     *
     * @param category
     * @param threshold only for zone which it's area below threshold.
     */
    public void fill(int category, int threshold) {
        setBlueprint(fill(blueprint(), category, new AreaThreshold(threshold)));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param blueprint
     * @param category
     * @param threshold only for zone which it's area below threshold.
     * @return filled result, a copied {@code blueprint}.
     */
    public int[] fill(int[] blueprint, int category, int threshold) {
        return fill(blueprint, category, new AreaThreshold(threshold));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param category
     * @param threshold
     */
    public void fill(int category, AreaThreshold threshold) {
        setBlueprint(fill(blueprint(), category, threshold));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param blueprint
     * @param category
     * @param threshold
     * @return filled result, a copied {@code blueprint}.
     */
    public int[] fill(int[] blueprint, int category, AreaThreshold threshold) {
        for (var clustering : getClusteringEdges(blueprint, category)) {
            if (threshold.test(Math.abs(clustering.area()))) {
                clustering = clustering.convex();
                fillClusteringEdges(blueprint, clustering);
            }
        }
        return blueprint;
    }

    public void extend(int category, int step) {
        setBlueprint(extend(blueprint(), category, step, AreaThreshold.ALL));
    }

    /**
     * @param blueprint
     * @param category
     * @param step
     * @return extended result, a copied {@code blueprint}.
     */
    public int[] extend(int[] blueprint, int category, int step) {
        return extend(blueprint, category, step, AreaThreshold.ALL);
    }

    public void extend(int category, int step, int threshold) {
        setBlueprint(extend(blueprint(), category, step, new AreaThreshold(threshold)));
    }

    /**
     * @param blueprint
     * @param category
     * @param step
     * @param threshold
     * @return extended result, a copied {@code blueprint}.
     */
    public int[] extend(int[] blueprint, int category, int step, int threshold) {
        return extend(blueprint, category, step, new AreaThreshold(threshold));
    }

    public void extend(int category, int step, AreaThreshold threshold) {
        setBlueprint(extend(blueprint(), category, step, threshold));
    }

    /**
     * @param blueprint
     * @param category
     * @param step
     * @param threshold
     * @return extended result, a copied {@code blueprint}.
     */
    public int[] extend(int[] blueprint, int category, int step, AreaThreshold threshold) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step == 0 || length == 0) return ret;

        return extend(ret, blueprint, category, step, threshold);
    }

    /**
     * extend category zones.
     * <br/>
     * This method does not reset the content of {@code output}.
     *
     * @param output    output blueprint
     * @param blueprint source blueprint
     * @param category
     * @param step
     * @param threshold
     * @return {@code output} itself.
     */
    public int[] extend(int[] output, int[] blueprint, int category, int step, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, category, true);
        var tmp = new int[blueprint.length];
        for (var group : clustering.groups()) {
            if (threshold.test(clustering.groupCount(group))) {
                Arrays.fill(tmp, ProbeDescription.CATE_UNSET);

                var src = new int[blueprint.length];
                clustering.isolate(blueprint, group, src);

                for (int i = -step; i <= step; i++) {
                    move(tmp, src, step, (_, _) -> 1);
                    or(output, tmp, category, output);
                }
            }
        }
        return output;
    }

    public void reduce(int category, int step) {
        setBlueprint(reduce(blueprint(), category, step, 0));
    }

    public int[] reduce(int[] blueprint, int category, int step) {
        return reduce(blueprint, category, step, 0);
    }

    public void reduce(int category, int step, int threshold) {
        setBlueprint(reduce(blueprint(), category, step, threshold));
    }

    public int[] reduce(int[] blueprint, int category, int step, int threshold) {
        //XXX Unsupported Operation BlueprintToolkit.reduce
        throw new UnsupportedOperationException();
    }
}
