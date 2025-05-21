package io.ast.jneurocarto.core.blueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.NullMarked;

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

    /*============================*
     * category zone manipulation *
     *============================*/

    public void move(int step) {
        if (step == 0 || length() == 0) return;
        setBlueprint(move(blueprint(), step));
    }

    public int[] move(int[] blueprint, int step) {
        if (length() != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step == 0 || length() == 0) return ret;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        for (int i = 0, length = blueprint.length; i < length; i++) {
            var cate = blueprint[i];
            if (cate != ProbeDescription.CATE_UNSET) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i] + step * dy;
                var j = index(s, x, y);
                if (j >= 0) {
                    ret[j] = cate;
                }
            }
        }

        return ret;
    }

    public void move(int step, int category) {
        if (step == 0 || length() == 0) return;
        setBlueprint(move(blueprint(), step, category));
    }

    public int[] move(int[] blueprint, int step, int category) {
        if (length() != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step == 0 || length() == 0) return ret;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        for (int i = 0, length = blueprint.length; i < length; i++) {
            var cate = blueprint[i];
            if (cate == category) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i] + step * dy;
                var j = index(s, x, y);
                if (j >= 0) {
                    ret[i] = ProbeDescription.CATE_UNSET;
                    ret[j] = cate;
                }
            }
        }

        return ret;
    }

    public void move(int step, int[] index) {
        if (step == 0 || length() == 0 || index.length == 0) return;
        setBlueprint(move(blueprint(), step, index));
    }

    public int[] move(int[] blueprint, int step, int[] index) {
        if (length() != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step == 0 || length() == 0 || index.length == 0) return ret;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        Arrays.sort(index);
        var pointer = 0;

        for (int i = 0, length = blueprint.length; i < length; i++) {
            if (pointer < index.length && index[pointer] == i) {
                pointer++;
                var cate = blueprint[i];
                if (cate != ProbeDescription.CATE_UNSET) {
                    var s = shank[i];
                    var x = posx[i];
                    var y = posy[i] + step * dy;
                    var j = index(s, x, y);
                    if (j >= 0 && Arrays.binarySearch(index, j) >= 0) {
                        ret[i] = ProbeDescription.CATE_UNSET;
                        ret[j] = cate;
                    }
                }
            }
        }

        return ret;
    }

    public void move(int step, boolean[] mask) {
        if (mask.length != length()) throw new IllegalArgumentException();
        setBlueprint(move(blueprint(), step, mask));
    }

    public int[] move(int[] blueprint, int step, boolean[] mask) {
        if (length() != blueprint.length) throw new RuntimeException();
        if (mask.length != length()) throw new IllegalArgumentException();

        var ret = blueprint.clone();
        if (step == 0 || length() == 0) return ret;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        for (int i = 0, length = blueprint.length; i < length; i++) {
            var cate = blueprint[i];
            if (cate != ProbeDescription.CATE_UNSET && mask[i]) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i] + step * dy;
                var j = index(s, x, y);
                if (j >= 0) {
                    ret[i] = ProbeDescription.CATE_UNSET;
                    ret[j] = cate;
                }
            }
        }

        return ret;
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

    public int[] fillClusteringEdges(int[] blueprint, List<ClusteringEdges> edges, int category) {
        var clustering = edges.stream().map(it -> it.withCategory(category)).toList();
        return fillClusteringEdges(blueprint, clustering);
    }

    public void fillClusteringEdges(ClusteringEdges edge) {
        fillClusteringEdges(blueprint(), edge);
        markDirty();
    }

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

    public void fill(int category) {
        fill(blueprint(), category, 0);
        markDirty();
    }

    public int[] fill(int[] blueprint, int category) {
        return fill(blueprint, category, 0);
    }

    public void fill(int category, int threshold) {
        fill(blueprint(), category, threshold);
        markDirty();
    }

    public int[] fill(int[] blueprint, int category, int threshold) {
        for (var clustering : getClusteringEdges(blueprint, category)) {
            if (Math.abs(clustering.area()) >= threshold) {
                clustering = clustering.convex();
                fillClusteringEdges(blueprint, clustering);
            }
        }
        return blueprint;
    }

    public void extend(int category, int step) {
        extend(blueprint(), category, step, 0);
        markDirty();
    }

    public int[] extend(int[] blueprint, int category, int step) {
        return extend(blueprint, category, step, 0);
    }

    public void extend(int category, int step, int threshold) {
        extend(blueprint(), category, step, threshold);
        markDirty();
    }

    public int[] extend(int[] blueprint, int category, int step, int threshold) {
        //XXX Unsupported Operation BlueprintToolkit.extend
        throw new UnsupportedOperationException();
    }

    public void reduce(int category, int step) {
        reduce(blueprint(), category, step, 0);
        markDirty();
    }

    public int[] reduce(int[] blueprint, int category, int step) {
        return reduce(blueprint, category, step, 0);
    }

    public void reduce(int category, int step, int threshold) {
        reduce(blueprint(), category, step, threshold);
        markDirty();
    }

    public int[] reduce(int[] blueprint, int category, int step, int threshold) {
        //XXX Unsupported Operation BlueprintToolkit.reduce
        throw new UnsupportedOperationException();
    }
}
