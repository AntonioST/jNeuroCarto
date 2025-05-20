package io.ast.jneurocarto.core.blueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Gatherer;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public final class BlueprintToolkit<T> {
    private final Blueprint<T> blueprint;

    public BlueprintToolkit(Blueprint<T> blueprint) {
        this.blueprint = blueprint;
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
        System.arraycopy(blueprint, 0, dst, 0, dst.length);
        markDirty();
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


    private static int indexOf(int[] array, int value) {
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] == value) return i;
        }
        return -1;
    }

    /*============================*
     * category zone manipulation *
     *============================*/

    public void move(int step) {
        if (step == 0 || length() == 0) return;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        var src = blueprint();
        var ret = newBlueprint();
        for (int i = 0, length = src.length; i < length; i++) {
            var cate = src[i];
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

        setBlueprint(ret);
    }

    public void move(int step, int category) {
        if (step == 0 || length() == 0) return;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        var src = blueprint();
        var ret = src.clone();
        for (int i = 0, length = src.length; i < length; i++) {
            var cate = src[i];
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

        setBlueprint(ret);
    }

    public void move(int step, int[] index) {
        if (step == 0 || length() == 0 || index.length == 0) return;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        Arrays.sort(index);
        var pointer = 0;

        var src = blueprint();
        var ret = src.clone();
        for (int i = 0, length = src.length; i < length; i++) {
            if (pointer < index.length && index[pointer] == i) {
                pointer++;
                var cate = src[i];
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

        setBlueprint(ret);
    }

    public void move(int step, boolean[] mask) {
        if (mask.length != length()) throw new IllegalArgumentException();

        if (step == 0 || length() == 0) return;

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        var src = blueprint();
        var ret = src.clone();
        for (int i = 0, length = src.length; i < length; i++) {
            var cate = src[i];
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

        setBlueprint(ret);
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
                output[i] = surrounding(s, x, y, i * 2);
                // 1, 3, 5, 7
                output[i + 1] = -1;
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
    public Clustering findClustering(boolean diagonal) {
        if (length() == 0) return Clustering.EMPTY;

        var src = blueprint();
        var minCate = Arrays.stream(src).min().orElse(ProbeDescription.CATE_UNSET);
        var maxCate = Arrays.stream(src).max().orElse(ProbeDescription.CATE_UNSET);

        var ret = new Clustering(length());

        // electrodes are belonging to same the category.
        if (minCate == maxCate) {
            if (minCate != ProbeDescription.CATE_UNSET) {
                ret.fill(1);
            }
            return ret;
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0, length = src.length; i < length; i++) {
            int cate = src[i];
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
        if (length() == 0) return Clustering.EMPTY;

        var src = blueprint();
        var n = (int) Arrays.stream(src).filter(i -> i == category).count();

        var ret = new Clustering(length());

        // electrodes are belonging to same the category.
        if (n == length()) {
            ret.fill(1);
            return ret;
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0, length = src.length; i < length; i++) {
            int cate = src[i];
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

        var clustering = findClustering(false);
        return getClusteringEdges(clustering);
    }

    public List<ClusteringEdges> getClusteringEdges(int category) {
        if (length() == 0) return List.of();

        var clustering = findClustering(category, false);
        return getClusteringEdges(clustering);
    }

    private List<ClusteringEdges> getClusteringEdges(Clustering clustering) {
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
                ret.add(pointClustering(c, s, x, y));
            } else {
                ret.add(areaClustering(c, s, Arrays.copyOfRange(index, 0, size)));
            }
        }

        return ret;
    }

    private static ClusteringEdges pointClustering(int c, int s, int x, int y) {
        return new ClusteringEdges(c, s, List.of(
          new ClusteringEdges.Corner(x, y, 1),
          new ClusteringEdges.Corner(x, y, 3),
          new ClusteringEdges.Corner(x, y, 5),
          new ClusteringEdges.Corner(x, y, 7)
        ));
    }

    private ClusteringEdges areaClustering(int c, int s, int[] index) {
        return new ClusteringEdges(c, s, walkClusteringArea(index));
    }

    private record Index(int i, int x, int y) {
        private static Gatherer<Index, ?, Index> minOn(ToIntFunction<Index> mapper) {
            return Gatherer.<Index, List<Index>, Index>ofSequential(
              ArrayList::new,
              (state, element, _) -> {
                  if (!state.isEmpty() && mapper.applyAsInt(element) < mapper.applyAsInt(state.getFirst())) {
                      state.clear();
                  }
                  state.add(element);
                  return true;
              },
              (state, downstream) -> state.forEach(downstream::push)
            );
        }
    }

    private record WalkAction(int direction, List<FollowAction> actions) {

        private static List<FollowAction> getAction(int direction) {
            for (var action : ACTIONS) {
                if (action.direction == direction) return action.actions;
            }
            throw new RuntimeException();
        }
    }

    private sealed interface FollowAction permits Turn, Back {
    }

    record Turn(int action, int corner) implements FollowAction {
    }

    record Back(int[] corners, int action) implements FollowAction {
    }

    private static WalkAction[] ACTIONS = {
      // WalkAction(direction=i->j)
      // rightward
      new WalkAction(0, List.of(
        // Turn(action=j->k, corner),
        // * * *
        // i j *
        //   k *
        new Turn(6, 5),
        // * * *
        // i j k
        // ?
        new Turn(0, 6),
        // * k
        // i j
        //
        new Turn(2, 7),
        // ?
        // i j
        //
        new Back(new int[]{7, 1, 3}, 2)
      )),
      // downward
      new WalkAction(6, List.of(
        //   i *
        // k j *
        // * * *
        new Turn(4, 3),
        // ? i *
        //   j *
        //   k *
        new Turn(6, 4),
        // ? i *
        //   j k
        //
        new Turn(0, 5),
        // ? i ?
        //   j
        //
        new Back(new int[]{5, 7, 1}, 0)
      )),
      // leftward
      new WalkAction(4, List.of(
        // ? k
        // * j i
        // * * *
        new Turn(2, 1),
        //
        // k j i
        // * * *
        new Turn(4, 2),
        //
        //   j i
        // ? k *
        new Turn(6, 3),
        //
        //  j i
        //    ?
        new Back(new int[]{3, 5, 7}, 6)
      )),
      // upward
      new WalkAction(2, List.of(
        // * * *
        // * j k
        // * i
        new Turn(0, 7),
        // * k
        // * j
        // * i
        new Turn(2, 0),
        //
        // k j
        // * i
        new Turn(4, 1),
        //
        //   j
        // ? i
        new Back(new int[]{1, 3, 5}, 4)
      )),
    };

    private List<ClusteringEdges.Corner> walkClusteringArea(int[] index) {
        if (index.length == 0) throw new IllegalArgumentException();

        var posx = posx();
        var posy = posy();

        var start = Arrays.stream(index)
          .mapToObj(i -> new Index(i, posx[i], posy[i]))
          .gather(Index.minOn(Index::x))
          .gather(Index.minOn(Index::y))
          .findFirst()
          .get();

        var ret = new ArrayList<ClusteringEdges.Corner>();

        // * ?
        // i * ?
        //   ? ?
        int i = start.i;
        int d = 0; // right
        int x = start.x;
        int y = start.y;
        ret.add(new ClusteringEdges.Corner(x, y, 5));

        while (!(start.i == i && d == 6)) {
            if (indexOf(index, i) < 0) throw new RuntimeException();

            x = posx[index[i]];
            y = posy[index[i]];

            loop:
            for (var actions : WalkAction.getAction(d)) {
                switch (actions) {
                case Turn(int action, int corner) -> {
                    var j = surrounding(index[i], action);
                    if (j >= 0) { // is index[i]+action located in probe?
                        var k = indexOf(index, j);
                        if (k >= 0) { // is index[i]+action located in index?
                            ret.add(new ClusteringEdges.Corner(x, y, corner));
                            i = k;
                            d = action;
                            break loop;
                        }
                    }
                }
                case Back(int[] corners, int action) -> {
                    for (var corner : corners) {
                        ret.add(new ClusteringEdges.Corner(x, y, corner));
                    }
                    d = action;
                    break loop;
                }
                }
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
            fillClusteringEdges(edge, blueprint);
        }
        markDirty();
    }

    public void fillClusteringEdges(List<ClusteringEdges> edges, int category) {
        fillClusteringEdges(edges.stream().map(it -> it.withCategory(category)).toList());
    }

    public void fillClusteringEdges(ClusteringEdges edge) {
        fillClusteringEdges(edge, blueprint());
        markDirty();
    }

    public int[] fillClusteringEdges(ClusteringEdges edge, int[] dst) {
        var c = edge.category();
        var s = edge.shank();
        var e = edge.setCorner(dx() / 2, dx() / 2);

        var shank = shank();
        var posx = posx();
        var posy = posy();

        for (int i = 0, length = dst.length; i < length; i++) {
            if (shank[i] == s) {
                var x = posx[i];
                var y = posy[i];
                if (e.contains(x, y)) {
                    dst[i] = c;
                }
            }
        }
        return dst;
    }

    /*====================*
     * fill category zone *
     *====================*/

    public void fill(int category) {
        fill(category, 0);
    }

    public void fill(int category, int threshold) {
        for (var clustering : getClusteringEdges(category)) {
            if (Math.abs(clustering.area()) >= threshold) {
                clustering = clustering.convex();
                fillClusteringEdges(clustering);
            }
        }
    }

    public void extend(int category, int step) {
        extend(category, step, 0);
    }

    public void extend(int category, int step, int threshold) {
    }

    public void reduce(int category, int step) {
        reduce(category, step, 0);
    }

    public void reduce(int category, int step, int threshold) {
    }
}
