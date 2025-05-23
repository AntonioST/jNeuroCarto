package io.ast.jneurocarto.core.blueprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Gatherer;
import java.util.stream.IntStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.numpy.Numpy;
import io.ast.jneurocarto.core.numpy.UnsupportedNumpyDataFormatException;

import static java.nio.file.StandardOpenOption.*;

@NullMarked
public class BlueprintToolkit<T> {

    /**
     * wrapped blueprint.
     */
    protected final Blueprint<T> blueprint;

    /**
     * warp {@link BlueprintToolkit} onto {@code blueprint}.
     *
     * @param blueprint
     */
    public BlueprintToolkit(Blueprint<T> blueprint) {
        this.blueprint = blueprint;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public BlueprintToolkit<T> clone() {
        return new BlueprintToolkit<>(new Blueprint<>(blueprint));
    }

    /*========*
     * getter *
     *========*/

    public ProbeDescription<T> probe() {
        return blueprint.probe;
    }

    public @Nullable T channelmap() {
        return blueprint.channelmap();
    }

    public final int length() {
        return blueprint.shank.length;
    }

    public final int[] shank() {
        return blueprint.shank;
    }

    public final int nShank() {
        return (int) Arrays.stream(shank()).distinct().count();
    }

    public final int[] posx() {
        return blueprint.posx;
    }

    public final int[] posy() {
        return blueprint.posy;
    }

    public final double dx() {
        return blueprint.dx;
    }

    public final double dy() {
        return blueprint.dy;
    }

    /*=================*
     * blueprint array *
     *=================*/

    public final int[] blueprint() {
        return blueprint.blueprint;
    }

    public final int[] newBlueprint() {
        var ret = new int[length()];
        Arrays.fill(ret, ProbeDescription.CATE_UNSET);
        return ret;
    }

    public final void setBlueprint(int[] blueprint) {
        var dst = this.blueprint.blueprint;
        if (blueprint.length != dst.length) throw new RuntimeException();
        if (dst != blueprint) {
            System.arraycopy(blueprint, 0, dst, 0, dst.length);
            markDirty();
        }
    }

    public final void setBlueprint(Blueprint<T> blueprint) {
        if (blueprint != this.blueprint) {
            setBlueprint(blueprint.blueprint);
        }
    }

    public final void setBlueprint(BlueprintToolkit<T> blueprint) {
        setBlueprint(blueprint.blueprint);
    }

    public final void mergeBlueprint(int[] blueprint) {
        var dst = this.blueprint.blueprint;
        if (blueprint.length != dst.length) throw new RuntimeException();
        if (dst != blueprint) {
            for (int i = 0, length = dst.length; i < length; i++) {
                if (dst[i] == ProbeDescription.CATE_UNSET) dst[i] = blueprint[i];
            }

            markDirty();
        }
    }

    public final void mergeBlueprint(Blueprint<T> blueprint) {
        if (blueprint != this.blueprint) {
            mergeBlueprint(blueprint.blueprint);
        }
    }

    public final void mergeBlueprint(BlueprintToolkit<T> blueprint) {
        mergeBlueprint(blueprint.blueprint);
    }

    public final boolean isDirty() {
        return blueprint.modified;
    }

    public final void markDirty() {
        blueprint.modified = true;
    }

    public final void syncBlueprint() {
        blueprint.syncBlueprint();
    }

    public final void applyBlueprint() {
        blueprint.applyBlueprint();
    }

    public final void printBlueprint() {
        printBlueprint(blueprint.blueprint);
    }

    public final String toStringBlueprint() {
        return toStringBlueprint(blueprint.blueprint);
    }

    public final void printBlueprint(int[] blueprint) {
        try {
            printBlueprint(blueprint, System.out);
        } catch (IOException e) {
        }
    }

    public final String toStringBlueprint(int[] blueprint) {
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

    /*===========================*
     * blueprint-like data array *
     *===========================*/

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

    public int[] index(T chmap) {
        return index(blueprint.probe.allChannels(chmap));
    }

    public int[] index(List<ElectrodeDescription> e) {
        var ret = new int[e.size()];
        for (int i = 0, length = ret.length; i < length; i++) {
            var t = e.get(i);
            ret[i] = index(t.s(), t.x(), t.y());
        }
        return ret;
    }

    public int[] index(Predicate<ElectrodeDescription> picker) {
        return blueprint.indexBlueprint(picker);
    }

    public boolean[] mask(T chmap) {
        return mask(blueprint.probe.allChannels(chmap));
    }

    public boolean[] mask(List<ElectrodeDescription> e) {
        var ret = new boolean[length()];
        for (int i = 0, length = length(); i < length; i++) {
            var t = e.get(i);
            var j = index(t.s(), t.x(), t.y());
            if (j >= 0) ret[j] = true;
        }
        return ret;
    }

    public boolean[] mask(Predicate<ElectrodeDescription> e) {
        return blueprint.maskBlueprint(e);
    }

    public boolean[] mask(int[] index) {
        var ret = new boolean[length()];
        for (int i = 0, length = length(); i < length; i++) {
            var j = index[i];
            if (j >= 0) ret[j] = true;
        }
        return ret;
    }

    public List<ElectrodeDescription> pick(int[] index) {
        var e = blueprint.electrodes;
        return Arrays.stream(index)
          .filter(it -> it >= 0)
          .mapToObj(e::get)
          .toList();
    }

    public List<ElectrodeDescription> pick(boolean[] mask) {
        if (length() != mask.length) throw new RuntimeException();
        var e = blueprint.electrodes;
        return IntStream.range(0, length())
          .filter(it -> mask[it])
          .mapToObj(e::get)
          .toList();
    }

    /**
     * {@code a[i] = v}
     *
     * @param a int array
     * @param i {@code a} indexed array
     * @param v value
     * @return {@code a} itself.
     */
    public int[] set(int[] a, int[] i, int v) {
        for (int j : i) {
            a[j] = v;
        }
        return a;
    }

    public int[] set(int[] a, int[] i, int[] v) {
        if (i.length != v.length) throw new IllegalArgumentException();
        for (int j = 0, length = i.length; j < length; j++) {
            a[i[j]] = v[j];

        }
        return a;
    }

    /**
     * {@code a[i[offset:offset+length]] = v}
     *
     * @param a      int array
     * @param i      {@code a} indexed array
     * @param offset
     * @param length
     * @param v      value
     * @return {@code a} itself.
     */
    public int[] set(int[] a, int[] i, int offset, int length, int v) {
        for (int j = 0; j < length; j++) {
            a[i[j + offset]] = v;
        }
        return a;
    }


    public int[] setIfUnset(int[] a, int[] i, int offset, int length, int v) {
        for (int j = 0; j < length; j++) {
            var k = i[j + offset];
            if (a[k] == 0) a[k] = v;
        }
        return a;
    }


    /*============================*
     * category zone manipulation *
     *============================*/

    /**
     * @param step y movement
     */
    public final void move(int step) {
        if (step == 0 || length() == 0) return;
        setBlueprint(move(blueprint(), step));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (step == 0) return blueprint.clone();

        var ret = new int[length];
        if (length() == 0) return ret;

        return move(ret, blueprint, step, (_, _) -> 1);
    }


    /**
     * @param step     y movement
     * @param category restrict on category
     */
    public final void move(int step, int category) {
        if (step == 0 || length() == 0) return;
        setBlueprint(move(blueprint(), step, category));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param category  restrict on category
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step, int category) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (step == 0) return blueprint.clone();

        var ret = new int[length];
        if (length() == 0) return ret;

        return move(ret, blueprint, step, (_, cate) -> cate == category ? 1 : 0);
    }

    /**
     * @param step  y movement
     * @param index electrode indexes
     */
    public final void move(int step, int[] index) {
        if (step == 0 || length() == 0 || index.length == 0) return;
        setBlueprint(move(blueprint(), step, index));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param index     electrode indexes
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step, int[] index) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (step == 0 || index.length == 0) return blueprint.clone();

        var ret = new int[length];
        if (length == 0) return ret;

        return move(ret, blueprint, step, index);
    }

    /**
     * @param step y movement
     * @param mask electrode mask array
     */
    public final void move(int step, boolean[] mask) {
        if (length() != mask.length) throw new IllegalArgumentException();
        setBlueprint(move(blueprint(), step, mask));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param mask      electrode mask array
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step, boolean[] mask) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (length != mask.length) throw new RuntimeException();
        if (step == 0) return blueprint.clone();

        var ret = new int[length];
        if (length() == 0) return ret;

        var x = ProbeDescription.CATE_UNSET;
        return move(ret, blueprint, step, (i, cate) -> (cate != x && mask[i]) ? 1 : 0);
    }

    private int[] move(int[] output, int[] blueprint, int step, int[] index) {
        Arrays.sort(index);
        var pointer = new AtomicInteger();

        return move(output, blueprint, step, (i, _) -> {
            var p = pointer.get();
            if (p < index.length && index[p] == i) {
                return pointer.incrementAndGet();
            } else {
                return 0;
            }
        });
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
    protected int[] move(int[] output, int[] blueprint, int step, IntBinaryOperator tester) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (length != output.length) throw new RuntimeException();
        if (length == 0) return output;
        if (step == 0) {
            if (output != blueprint) {
                System.arraycopy(blueprint, 0, output, 0, length);
            }
            return output;
        }

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
                var y = (int) (posy[i] + step * dy);
                j = index(s, x, y);
            }

            if (j >= 0 && output[j] == ProbeDescription.CATE_UNSET) {
                output[j] = cate;
            }
        }

        return output;
    }

    /**
     * move electrodes index along y-axis.
     *
     * @param index electrode index.
     * @param step  y movement
     * @return moved electrode index. {@code -1} when it outside the probe.
     */
    public int[] moveIndex(int[] index, int step) {
        if (index.length == 0) return index;
        if (step == 0) {
            return index;
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();
        var ret = new int[index.length];

        for (int i = 0, length = index.length; i < length; i++) {
            int j = index[i];

            var s = shank[j];
            var x = posx[j];
            var y = (int) (posy[j] + step * dy);
            ret[i] = index(s, x, y);
        }

        return ret;
    }

    /**
     * move electrodes index along y-axis.
     *
     * @param output output electrode index.
     * @param index  source electrode index.
     * @param step   y movement
     * @return number of index filled in {@code output}.
     */
    protected int moveIndex(int[] output, int[] index, int step) {
        if (index.length == 0) return 0;
        if (step == 0) {
            System.arraycopy(index, 0, output, 0, index.length);
            return index.length;
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();
        var ret = 0;

        for (var i : index) {
            var s = shank[i];
            var x = posx[i];
            var y = (int) (posy[i] + step * dy);
            var j = index(s, x, y);

            if (j >= 0) {
                output[ret++] = j;
            }
        }

        return ret;
    }

    /*=======================*
     * electrode surrounding *
     *=======================*/

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
                output[2 * i] = surrounding(s, x, y, i * 2);
                // 1, 3, 5, 7
                output[2 * i + 1] = -1;
            }
        }
    }

    public int surrounding(int electrode, int c) {
        var s = shank()[electrode];
        var x = posx()[electrode];
        var y = posy()[electrode];
        return surrounding(s, x, y, c);
    }

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
    public final Clustering findClustering(boolean diagonal) {
        return findClustering(blueprint(), diagonal);
    }

    public Clustering findClustering(int[] blueprint, boolean diagonal) {
        var length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        var x = ProbeDescription.CATE_UNSET;
        return findClustering(blueprint, diagonal, (_, cate) -> cate != x ? 1 : 0);
    }

    public final Clustering findClustering(int category, boolean diagonal) {
        return findClustering(blueprint(), category, diagonal);
    }

    public Clustering findClustering(int[] blueprint, int category, boolean diagonal) {
        int length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        return findClustering(blueprint, diagonal, (_, cate) -> cate == category ? 1 : 0);
    }

    protected Clustering findClustering(int[] blueprint, boolean diagonal, IntBinaryOperator tester) {
        int length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        var n = (int) Arrays.stream(blueprint)
          .filter(i -> tester.applyAsInt(i, blueprint[i]) > 0)
          .count();

        var ret = new Clustering(length);
        if (n == 0) {
            return ret;
        } else if (n == length) {  // electrodes are belonging to same the category.
            ret.fill(1);
            return ret;
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0; i < length; i++) {
            int cate = blueprint[i];
            if (tester.applyAsInt(i, cate) > 0) {
                ret.set(i, group++);

                surrounding(i, diagonal, surr);
                for (var j : surr) {
                    if (j >= 0 && blueprint[j] == cate) {
                        ret.unionClusteringGroup(i, j);
                    }
                }
            }
        }

        return ret;
    }


    public final List<ClusteringEdges> getClusteringEdges() {
        if (length() == 0) return List.of();
        return getClusteringEdges(findClustering(false));
    }

    public final List<ClusteringEdges> getClusteringEdges(int[] blueprint) {
        if (length() != blueprint.length) throw new RuntimeException();
        if (blueprint.length == 0) return List.of();
        var clustering = findClustering(blueprint, false);
        return getClusteringEdges(clustering);
    }

    public final List<ClusteringEdges> getClusteringEdges(int category) {
        if (length() == 0) return List.of();
        return getClusteringEdges(findClustering(category, false));
    }

    public final List<ClusteringEdges> getClusteringEdges(int[] blueprint, int category) {
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
            var size = clustering.indexGroup(g, index);
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

    public final void fillClusteringEdges(List<ClusteringEdges> edges) {
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
    public final int[] fillClusteringEdges(int[] blueprint, List<ClusteringEdges> edges) {
        for (var edge : edges) {
            fillClusteringEdges(blueprint, edge);
        }
        return blueprint;
    }

    public final void fillClusteringEdges(List<ClusteringEdges> edges, int category) {
        var clustering = edges.stream().map(it -> it.withCategory(category)).toList();
        fillClusteringEdges(clustering);
    }

    /**
     * @param blueprint output blueprint.
     * @param edges
     * @param category  overwrite category.
     * @return
     */
    public final int[] fillClusteringEdges(int[] blueprint, List<ClusteringEdges> edges, int category) {
        var clustering = edges.stream().map(it -> it.withCategory(category)).toList();
        return fillClusteringEdges(blueprint, clustering);
    }

    public final void fillClusteringEdges(ClusteringEdges edge) {
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

    public record AreaThreshold(int lower, int upper) implements DoublePredicate {
        public static final AreaThreshold ALL = new AreaThreshold(0, Integer.MAX_VALUE);

        public AreaThreshold(int upper) {
            this(0, upper);
        }

        @Override
        public boolean test(double value) {
            return lower <= value && value < upper;
        }
    }

    private record MinMaxInt(int min, int max) {
        MinMaxInt(int value) {
            this(value, value);
        }

        MinMaxInt consume(MinMaxInt other) {
            return new MinMaxInt(Math.min(min, other.min), Math.max(max, other.max));
        }

        static Gatherer<Integer, ?, MinMaxInt> minmax() {
            return Gatherer.ofSequential(
              () -> new MinMaxInt[1],
              Gatherer.Integrator.ofGreedy((state, element, _) -> {
                  var minmax = new MinMaxInt(element);
                  if (state[0] == null) {
                      state[0] = minmax;
                  } else {
                      state[0] = state[0].consume(minmax);
                  }
                  return true;
              }),
              (state, downstream) -> downstream.push(state[0])
            );
        }
    }

    /**
     * fill all category zones as rectangle.
     */
    public final void fill() {
        setBlueprint(fill(blueprint(), AreaThreshold.ALL));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param category
     */
    public final void fill(int category) {
        setBlueprint(fill(blueprint(), category, AreaThreshold.ALL));
    }

    /**
     * fill all category zones as rectangle.
     *
     * @param blueprint
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint) {
        return fill(blueprint, AreaThreshold.ALL);
    }

    /**
     * fill category zone as rectangle.
     *
     * @param blueprint
     * @param category
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint, int category) {
        return fill(blueprint, category, AreaThreshold.ALL);
    }

    /**
     * fill all category zones as rectangle.
     *
     * @param threshold
     */
    public final void fill(AreaThreshold threshold) {
        setBlueprint(fill(blueprint(), threshold));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param category
     * @param threshold
     */
    public final void fill(int category, AreaThreshold threshold) {
        setBlueprint(fill(blueprint(), category, threshold));
    }

    /**
     * fill all category zones as rectangle.
     *
     * @param blueprint
     * @param threshold
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, true);
        return fill(blueprint, clustering, threshold);
    }

    /**
     * fill category zone as rectangle.
     *
     * @param blueprint
     * @param category
     * @param threshold
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint, int category, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, category, true);
        return fill(blueprint, clustering, threshold);
    }

    public int[] fill(int[] blueprint, Clustering clustering, AreaThreshold threshold) {
        var shank = shank();
        var posx = posx();
        var posy = posy();

        for (var group : clustering.groups()) {
            if (threshold.test(clustering.groupCount(group))) {
                var index = clustering.indexGroup(group);
                var s = shank[index[0]];
                var c = blueprint[index[0]];
                var x = Arrays.stream(index).map(i -> posx[i]).boxed().gather(MinMaxInt.minmax()).findFirst().get();
                var y = Arrays.stream(index).map(i -> posy[i]).boxed().gather(MinMaxInt.minmax()).findFirst().get();

                fillClusteringEdges(blueprint, new ClusteringEdges(c, s, List.of(
                  new ClusteringEdges.Corner(x.max, y.max, 1),
                  new ClusteringEdges.Corner(x.min, y.max, 3),
                  new ClusteringEdges.Corner(x.min, y.min, 5),
                  new ClusteringEdges.Corner(x.max, y.min, 7)
                )));
            }
        }
        return blueprint;
    }

    public final void extend(int category, int step) {
        setBlueprint(extend(blueprint(), category, step, category, AreaThreshold.ALL));
    }

    public final void extend(int category, int step, int value) {
        setBlueprint(extend(blueprint(), category, step, value, AreaThreshold.ALL));
    }

    /**
     * @param blueprint
     * @param category
     * @param step
     * @return extended result, a copied {@code blueprint}.
     */
    public final int[] extend(int[] blueprint, int category, int step) {
        return extend(blueprint, category, step, category, AreaThreshold.ALL);
    }

    public final int[] extend(int[] blueprint, int category, int step, int value) {
        return extend(blueprint, category, step, value, AreaThreshold.ALL);
    }


    /**
     * @param category
     * @param step
     * @param threshold
     * @return extended result, a copied {@code blueprint}.
     */
    public final void extend(int category, int step, AreaThreshold threshold) {
        setBlueprint(extend(blueprint(), category, step, category, threshold));
    }

    public final void extend(int category, int step, int value, AreaThreshold threshold) {
        setBlueprint(extend(blueprint(), category, step, value, threshold));
    }

    /**
     * @param blueprint
     * @param category
     * @param step
     * @param threshold
     * @return extended result, a copied {@code blueprint}.
     */
    public final int[] extend(int[] blueprint, int category, int step, AreaThreshold threshold) {
        return extend(blueprint, category, step, category, threshold);
    }

    public final int[] extend(int[] blueprint, int category, int step, int value, AreaThreshold threshold) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step == 0 || length == 0) return ret;

        return extend(ret, blueprint, category, step, value, threshold);
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
    protected int[] extend(int[] output, int[] blueprint, int category, int step, int value, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var index = clustering.indexGroup();
        var move = new int[index.length];
        for (int i = -step; i <= step; i++) {
            var size = moveIndex(move, index, i);
            setIfUnset(output, move, 0, size, value);
        }
        return output;
    }

    public final void reduce(int category, int step) {
        setBlueprint(reduce(blueprint(), category, step, ProbeDescription.CATE_UNSET, AreaThreshold.ALL));
    }

    public final void reduce(int category, int step, int value) {
        setBlueprint(reduce(blueprint(), category, step, value, AreaThreshold.ALL));
    }

    public final int[] reduce(int[] blueprint, int category, int step) {
        return reduce(blueprint, category, step, ProbeDescription.CATE_UNSET, AreaThreshold.ALL);
    }

    public final int[] reduce(int[] blueprint, int category, int value, int step) {
        return reduce(blueprint, category, step, value, AreaThreshold.ALL);
    }

    public final void reduce(int category, int step, AreaThreshold threshold) {
        setBlueprint(reduce(blueprint(), category, step, ProbeDescription.CATE_UNSET, threshold));
    }

    public final void reduce(int category, int step, int value, AreaThreshold threshold) {
        setBlueprint(reduce(blueprint(), category, step, value, threshold));
    }

    public final int[] reduce(int[] blueprint, int category, int step, AreaThreshold threshold) {
        return reduce(blueprint, category, step, ProbeDescription.CATE_UNSET, threshold);
    }

    public final int[] reduce(int[] blueprint, int category, int step, int value, AreaThreshold threshold) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step == 0 || length == 0) return ret;

        return reduce(ret, blueprint, category, step, value, threshold);
    }

    /**
     * reduce category zones.
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
    protected int[] reduce(int[] output, int[] blueprint, int category, int step, int value, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var index = clustering.indexGroup();
        var move = new int[index.length];

        var size = moveIndex(move, index, -step);
        Arrays.sort(move, 0, size);
        for (int j : index) {
            if (Arrays.binarySearch(move, j) < 0) {
                output[j] = value;
            }
        }

        size = moveIndex(move, index, step);
        Arrays.sort(move, 0, size);
        for (int j : index) {
            if (Arrays.binarySearch(move, j) < 0) {
                output[j] = value;
            }
        }

        return output;
    }

    /*=========*
     * File IO *
     *=========*/

    public int[] loadBlueprint(Path file) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);
        if (suffix.equals(".npy")) {
            try {
                return loadNumpyBlueprint(file);
            } catch (UnsupportedNumpyDataFormatException e) {
            }
        } else if (suffix.equals(".csv")) {
            return loadCsvBlueprint(file, false);
        } else if (suffix.equals(".tsv")) {
            return loadCsvBlueprint(file, true);
        }

        var bp = new Blueprint<>(blueprint);
        bp.load(file);
        return bp.blueprint;
    }

    public int[] loadNumpyBlueprint(Path file) throws IOException {
        var result = Numpy.read(file, header -> switch (header.ndim()) {
            case 1 -> Numpy.ofInt();
            case 2 -> Numpy.ofD2Int(true);
            default -> throw new UnsupportedNumpyDataFormatException(header, "unknown shape");
        });

        return switch (result.ndim()) {
            case 1 -> (int[]) result.data();
            case 2 -> ((int[][]) result.data())[4];
            default -> throw new RuntimeException("unknown result");
        };
    }

    public void saveNumpyBlueprint(Path file, int[] blueprint) throws IOException {
        Numpy.write(file, blueprint);
    }

    public int[] loadCsvBlueprint(Path file) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);
        if (suffix.equals(".csv")) {
            return loadCsvBlueprint(file, false);
        } else if (suffix.equals(".tsv")) {
            return loadCsvBlueprint(file, true);
        } else {
            throw new RuntimeException("unknown file format : " + filename);
        }
    }

    private int[] loadCsvBlueprint(Path file, boolean tsv) throws IOException {
        var format = getCsvFormat(tsv).get();

        try (var reader = Files.newBufferedReader(file)) {
            var parse = format.parse(reader);
            var header = parse.getHeaderNames();
            if (!checkCsvHeader(header)) throw new IOException("unknown header : " + header);

            var ret = newBlueprint();
            for (var record : parse) {
                int s, x, y, c;
                try {
                    s = Integer.parseInt(record.get(header.get(0)));
                    x = Integer.parseInt(record.get(header.get(1)));
                    y = Integer.parseInt(record.get(header.get(2)));
                    c = Integer.parseInt(record.get(header.get(3)));
                } catch (NumberFormatException e) {
                    var line = parse.getCurrentLineNumber();
                    throw new IOException("bad numbers at line " + line, e);
                }

                var i = index(s, x, y);
                if (i >= 0) {
                    ret[i] = c;
                }
            }

            return ret;
        }
    }

    private static CSVFormat.Builder getCsvFormat(boolean tsv) {
        return CSVFormat.DEFAULT.builder()
          .setCommentMarker('#')
          .setIgnoreSurroundingSpaces(true)
          .setIgnoreHeaderCase(true)
          .setDelimiter(tsv ? '\t' : ',');
    }

    private static boolean checkCsvHeader(List<String> header) {
        if (header.size() != 4) return false;
        var field = header.get(0);
        if (!(field.equals("s") || field.equals("shank"))) {
            return false;
        }
        field = header.get(1);
        if (!field.equals("x")) return false;
        field = header.get(2);
        if (!field.equals("y")) return false;
        return true;
    }

    public void saveCsvBlueprint(Path file, int[] blueprint) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);
        if (suffix.equals(".csv")) {
            saveCsvBlueprint(file, blueprint, false);
        } else if (suffix.equals(".tsv")) {
            saveCsvBlueprint(file, blueprint, true);
        } else {
            throw new RuntimeException("unknown file format : " + filename);
        }
    }

    private void saveCsvBlueprint(Path file, int[] blueprint, boolean tsv) throws IOException {
        if (length() != blueprint.length) throw new IllegalArgumentException();

        var format = getCsvFormat(tsv)
          .setHeader("s", "x", "y", "c")
          .get();

        try (var writer = Files.newBufferedWriter(file, CREATE, TRUNCATE_EXISTING, WRITE);
             var printer = new CSVPrinter(writer, format)) {
            var shank = shank();
            var posx = posx();
            var posy = posy();

            for (int i = 0, length = blueprint.length; i < length; i++) {
                printer.printRecord(shank[i], posx[i], posy[i], blueprint[i]);
            }
        }
    }

    /*==================================*
     * blueprint-like data manipulation *
     *==================================*/

    /**
     * extra value from {@code data} with given index.
     * <br/>
     * If index out of array boundary, {@link Double#NaN} is used.
     *
     * @param data
     * @param index
     * @return
     */
    public double[] get(double[] data, int[] index) {
        var ret = new double[index.length];
        for (int i = 0, length = index.length; i < length; i++) {
            var j = index[i];
            if (j >= 0 && j < length) {
                ret[i] = data[j];
            } else {
                ret[i] = Double.NaN;
            }
        }
        return ret;
    }

    /**
     * extra value from {@code data} with given channelmap electrodes.
     * <br/>
     * If index out of array boundary, {@link Double#NaN} is used.
     *
     * @param data
     * @param chmap
     * @return
     */
    public double[] get(double[] data, T chmap) {
        return get(data, blueprint.probe.allChannels(chmap));
    }

    /**
     * extra value from {@code data} with given electrodes.
     * <br/>
     * If index out of array boundary, {@link Double#NaN} is used.
     *
     * @param data
     * @param e
     * @return
     */
    public double[] get(double[] data, List<ElectrodeDescription> e) {
        return e.stream()
          .mapToInt(it -> index(it.s(), it.x(), it.y()))
          .mapToDouble(i -> i < 0 ? Double.NaN : data[i])
          .toArray();
    }

    /**
     * {@code a[i] = v}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param a double array
     * @param i {@code a} indexed array
     * @param v value
     * @return {@code a} itself.
     */
    public double[] set(double[] a, int[] i, double v) {
        for (int j = 0, length = i.length; j < length; j++) {
            var k = i[j];
            if (k >= 0 && k < length) {
                a[k] = v;
            }
        }
        return a;
    }

    public double[] set(double[] a, T chmap, double v) {
        return set(a, blueprint.probe.allChannels(chmap), v);
    }

    public double[] set(double[] a, List<ElectrodeDescription> e, double v) {
        for (var k : e) {
            var i = index(k.s(), k.x(), k.y());
            if (i >= 0 && i < a.length) {
                a[i] = v;
            }
        }
        return a;
    }

    /**
     * {@code a[i] = v[:]}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param a
     * @param i
     * @param v
     * @return {@code a} itself
     * @throws IllegalArgumentException the length of {@code i} and {@code v} does not agree.
     */
    public double[] set(double[] a, int[] i, double[] v) {
        if (i.length != v.length) throw new IllegalArgumentException();
        for (int j = 0, length = i.length; j < length; j++) {
            var k = i[j];
            if (k >= 0 && k < length) {
                a[k] = v[j];
            }
        }
        return a;
    }

    /**
     * {@code a[i[offset:offset+length]] = v}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param a      double array
     * @param i      {@code a} indexed array
     * @param offset start index of {@code i}
     * @param length
     * @param v      value
     * @return {@code a} itself.
     * @throws IllegalArgumentException       when negative {@code length}
     * @throws ArrayIndexOutOfBoundsException {@code offset} and {@code length} out of {@code i}'s bounds.
     */
    public double[] set(double[] a, int[] i, int offset, int length, double v) {
        if (length < 0) throw new IllegalArgumentException();
        var _ = i[offset];
        var _ = i[offset + length - 1];

        for (int j = 0; j < length; j++) {
            var k = i[j + offset];
            if (k >= 0 && k < length) {
                a[k] = v;
            }
        }
        return a;
    }


    /**
     * {@code a[i[offset:offset+length]] = oper(a[i[offset:offset+length]])}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param a      double array
     * @param i      {@code a} indexed array
     * @param offset
     * @param length
     * @param oper   value operator
     * @return {@code a} itself.
     */
    public double[] set(double[] a, int[] i, int offset, int length, DoubleUnaryOperator oper) {
        if (length < 0) throw new IllegalArgumentException();
        var _ = i[offset];
        var _ = i[offset + length - 1];

        for (int j = 0; j < length; j++) {
            var k = i[j + offset];
            if (k >= 0 && k < length) {
                a[k] = oper.applyAsDouble(a[k]);
            }
        }
        return a;
    }

    public enum InterpolateNaNBuiltinMethod {
        zero, mean, median, min, max;

    }

    private static ToDoubleFunction<double[]> getInterpolateMethod(InterpolateNaNBuiltinMethod f) {
        return switch (f) {
            case zero -> _ -> 0;
            case mean -> kernel -> {
                double ret = 0;
                int cnt = 0;
                for (double v : kernel) {
                    if (!Double.isNaN(v)) {
                        ret += v;
                        cnt++;
                    }
                }
                return cnt == 0 ? Double.NaN : ret / cnt;
            };
            case median -> kernel -> {
                Arrays.sort(kernel); // NaN are put at last.
                var size = kernel.length;
                while (size > 0 && Double.isNaN(kernel[size - 1])) {
                    size--;
                }
                if (size == 0) return Double.NaN;
                if (size == 1) return kernel[0];
                if (size % 2 == 1) {
                    return kernel[size / 2];
                } else {
                    var i = size / 2;
                    return (kernel[i] + kernel[i - 1]) / 2;
                }
            };
            case min -> kernel -> {
                double ret = Double.MAX_VALUE;
                int cnt = 0;
                for (double v : kernel) {
                    if (!Double.isNaN(v)) {
                        ret = Math.min(ret, v);
                        cnt++;
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
            case max -> kernel -> {
                double ret = Double.MIN_VALUE;
                int cnt = 0;
                for (double v : kernel) {
                    if (!Double.isNaN(v)) {
                        ret = Math.max(ret, v);
                        cnt++;
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
        };
    }

    public double[] interpolateNaN(double[] a, int k, InterpolateNaNBuiltinMethod f) {
        var m = getInterpolateMethod(f);
        return interpolateNaN(a, k, m);
    }

    public final double[] interpolateNaN(double[] a, int k, ToDoubleFunction<double[]> f) {
        if (k % 2 != 1) throw new IllegalArgumentException();
        return interpolateNaN(a, new double[k], f);
    }

    public final double[] interpolateNaN(double[] a, double[] k, ToDoubleFunction<double[]> f) {
        return interpolateNaN(new double[a.length], a, k, f);
    }

    public double[] interpolateNaN(double[] o, double[] a, double[] k, ToDoubleFunction<double[]> f) {
        if (k.length % 2 != 1) throw new IllegalArgumentException();

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        for (int i = 0, length = a.length; i < length; i++) {
            if (Double.isNaN(a[i])) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i];

                for (int j = 0, kn = k.length; j < kn; j++) {
                    var n = j - kn / 2;
                    if (n == 0) {
                        k[j] = Double.NaN;
                    } else {
                        var yy = (int) (y + dy * n);
                        var jj = index(s, x, yy);
                        if (jj >= 0) {
                            k[j] = a[jj];
                        } else {
                            k[j] = Double.NaN;
                        }
                    }
                }

                o[i] = f.applyAsDouble(k);
            } else {
                o[i] = a[i];
            }
        }
        return a;
    }

    private static ToDoubleFunction<double[][]> getInterpolateMethod(int kx, int ky, InterpolateNaNBuiltinMethod f) {
        return switch (f) {
            case zero -> _ -> 0;
            case mean -> kernel -> {
                double ret = 0;
                int cnt = 0;
                for (double[] vx : kernel) {
                    for (var v : vx) {
                        if (!Double.isNaN(v)) {
                            ret += v;
                            cnt++;
                        }
                    }
                }
                return cnt == 0 ? Double.NaN : ret / cnt;
            };
            case median -> new ToDoubleFunction<>() {
                private final double[] tmp = new double[kx * ky];
                private final ToDoubleFunction<double[]> func = getInterpolateMethod(InterpolateNaNBuiltinMethod.median);

                @Override
                public double applyAsDouble(double[][] kernel) {
                    var p = 0;
                    for (int i = 0; i < kx; i++) {
                        for (int j = 0; j < ky; j++) {
                            tmp[p++] = kernel[i][j];
                        }
                    }

                    return func.applyAsDouble(tmp);
                }
            };
            case min -> kernel -> {
                double ret = Double.MAX_VALUE;
                int cnt = 0;
                for (double[] vv : kernel) {
                    for (var v : vv) {
                        if (!Double.isNaN(v)) {
                            ret = Math.min(ret, v);
                            cnt++;
                        }
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
            case max -> kernel -> {
                double ret = Double.MIN_VALUE;
                int cnt = 0;
                for (double[] vv : kernel) {
                    for (var v : vv) {
                        if (!Double.isNaN(v)) {
                            ret = Math.max(ret, v);
                            cnt++;
                        }
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
        };
    }


    public double[] interpolateNaN(double[] a, int kx, int ky, InterpolateNaNBuiltinMethod f) {
        var m = getInterpolateMethod(kx, ky, f);
        return interpolateNaN(a, kx, ky, m);
    }

    public final double[] interpolateNaN(double[] a, int kx, int ky, ToDoubleFunction<double[][]> f) {
        if (kx % 2 != 1) throw new IllegalArgumentException();
        if (ky % 2 != 1) throw new IllegalArgumentException();
        var k = new double[kx][];
        for (int i = 0; i < kx; i++) {
            k[i] = new double[ky];
        }
        return interpolateNaN(a, k, f);
    }

    public final double[] interpolateNaN(double[] a, double[][] k, ToDoubleFunction<double[][]> f) {
        return interpolateNaN(new double[a.length], a, k, f);
    }

    public double[] interpolateNaN(double[] o, double[] a, double[][] k, ToDoubleFunction<double[][]> f) {
        var kx = k.length;
        if (kx % 2 != 1) throw new IllegalArgumentException();
        var ky = k[0].length;
        if (ky % 2 != 1) throw new IllegalArgumentException();
        for (int i = 1; i < kx; i++) {
            if (k[i].length != ky) throw new IllegalArgumentException();
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dx = dx();
        var dy = dy();

        for (int i = 0, length = a.length; i < length; i++) {
            if (Double.isNaN(a[i])) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i];

                for (int jx = 0; jx < kx; jx++) {
                    for (int jy = 0; jy < ky; jy++) {
                        var nx = jx - kx / 2;
                        var ny = jy - ky / 2;
                        if (nx == 0 && ny == 0) {
                            k[jx][jy] = Double.NaN;
                        } else {
                            var xx = (int) (x + dx * nx);
                            var yy = (int) (y + dy * ny);
                            var jj = index(s, xx, yy);
                            if (jj >= 0) {
                                k[jx][jy] = a[jj];
                            } else {
                                k[jx][jy] = Double.NaN;
                            }
                        }
                    }
                }

                o[i] = f.applyAsDouble(k);
            } else {
                o[i] = a[i];
            }
        }
        return a;
    }

}
