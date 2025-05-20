package io.ast.jneurocarto.probe_npx.blueprint;

import java.util.*;
import java.util.stream.Gatherer;

import io.ast.jneurocarto.core.Blueprint;
import io.ast.jneurocarto.core.BlueprintTool;
import io.ast.jneurocarto.core.ProbeDescription;

public final class BlueprintUtils {
    private BlueprintUtils() {
        throw new RuntimeException();
    }

    public static <T> void move(Blueprint<T> blueprint, int step) {
        var tool = new BlueprintTool<>(blueprint);
        if (step == 0 || tool.length() == 0) return;

        var shank = tool.shank();
        var posx = tool.posx();
        var posy = tool.posy();
        var dy = tool.dy();

        var src = tool.blueprint();
        var ret = tool.newBleurptint();
        for (int i = 0, length = src.length; i < length; i++) {
            var cate = src[i];
            if (cate != ProbeDescription.CATE_UNSET) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i] + step * dy;
                var j = tool.index(s, x, y);
                if (j >= 0) {
                    ret[j] = cate;
                }
            }
        }

        tool.setBlueprint(ret);
    }

    public static <T> void move(Blueprint<T> blueprint, int step, int category) {
        var tool = new BlueprintTool<>(blueprint);
        if (step == 0 || tool.length() == 0) return;

        var shank = tool.shank();
        var posx = tool.posx();
        var posy = tool.posy();
        var dy = tool.dy();

        var src = tool.blueprint();
        var ret = src.clone();
        for (int i = 0, length = src.length; i < length; i++) {
            var cate = src[i];
            if (cate == category) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i] + step * dy;
                var j = tool.index(s, x, y);
                if (j >= 0) {
                    ret[i] = ProbeDescription.CATE_UNSET;
                    ret[j] = cate;
                }
            }
        }

        tool.setBlueprint(ret);
    }

    public static <T> void move(Blueprint<T> blueprint, int step, int[] index) {
        var tool = new BlueprintTool<>(blueprint);
        if (step == 0 || tool.length() == 0 || index.length == 0) return;

        var shank = tool.shank();
        var posx = tool.posx();
        var posy = tool.posy();
        var dy = tool.dy();

        Arrays.sort(index);
        var pointer = 0;

        var src = tool.blueprint();
        var ret = src.clone();
        for (int i = 0, length = src.length; i < length; i++) {
            if (pointer < index.length && index[pointer] == i) {
                pointer++;
                var cate = src[i];
                if (cate != ProbeDescription.CATE_UNSET) {
                    var s = shank[i];
                    var x = posx[i];
                    var y = posy[i] + step * dy;
                    var j = tool.index(s, x, y);
                    if (j >= 0 && Arrays.binarySearch(index, j) >= 0) {
                        ret[i] = ProbeDescription.CATE_UNSET;
                        ret[j] = cate;
                    }
                }
            }
        }

        tool.setBlueprint(ret);
    }

    public static <T> void move(Blueprint<T> blueprint, int step, boolean[] mask) {
        var tool = new BlueprintTool<>(blueprint);
        if (mask.length != tool.length()) throw new IllegalArgumentException();

        if (step == 0 || tool.length() == 0) return;

        var shank = tool.shank();
        var posx = tool.posx();
        var posy = tool.posy();
        var dy = tool.dy();

        var src = tool.blueprint();
        var ret = src.clone();
        for (int i = 0, length = src.length; i < length; i++) {
            var cate = src[i];
            if (cate != ProbeDescription.CATE_UNSET && mask[i]) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i] + step * dy;
                var j = tool.index(s, x, y);
                if (j >= 0) {
                    ret[i] = ProbeDescription.CATE_UNSET;
                    ret[j] = cate;
                }
            }
        }

        tool.setBlueprint(ret);
    }

    /**
     * @param blueprint a blueprint which has {@code E} electrodes.
     * @param diagonal  does surrounding includes electrodes on diagonal?
     * @return {@code E}-length int-array that the surrounding electrode shared same positive int value.
     */
    public static <T> int[] findClustering(Blueprint<T> blueprint, boolean diagonal) {
        var tool = new BlueprintTool<>(blueprint);
        if (tool.length() == 0) return new int[0];

        var src = tool.blueprint();
        var minCate = Arrays.stream(src).min().orElse(ProbeDescription.CATE_UNSET);
        var maxCate = Arrays.stream(src).max().orElse(ProbeDescription.CATE_UNSET);

        var ret = new int[tool.length()];

        // electrodes are belonging to same the category.
        if (minCate == maxCate) {
            if (minCate != ProbeDescription.CATE_UNSET) {
                Arrays.fill(ret, 1);
            }
            return ret;
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0, length = src.length; i < length; i++) {
            int cate = src[i];
            if (cate != ProbeDescription.CATE_UNSET) {
                ret[i] = group++;

                Surroundings.surrounding(tool, i, diagonal, surr);
                for (int k = 0; k < 8; k++) {
                    var j = surr[k];
                    if (j >= 0) {
                        unionGroupIdentify(ret, i, j);
                    }
                }
            }
        }

        return ret;
    }

    public static <T> int[] findClustering(Blueprint<T> blueprint, int category, boolean diagonal) {
        var tool = new BlueprintTool<>(blueprint);
        if (tool.length() == 0) return new int[0];

        var src = tool.blueprint();
        var n = (int) Arrays.stream(src).filter(i -> i == category).count();

        var ret = new int[tool.length()];

        // electrodes are belonging to same the category.
        if (n == tool.length()) {
            Arrays.fill(ret, 1);
            return ret;
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0, length = src.length; i < length; i++) {
            int cate = src[i];
            if (cate == category) {
                ret[i] = group++;

                Surroundings.surrounding(tool, i, diagonal, surr);
                for (int k = 0; k < 8; k++) {
                    var j = surr[k];
                    if (j >= 0) {
                        unionGroupIdentify(ret, i, j);
                    }
                }
            }
        }

        return ret;
    }


    private static void unionGroupIdentify(int[] blueprint, int i, int j) {
        if (i == j) return;

        var a = blueprint[i];
        var b = blueprint[j];
        if (a == 0 && b == 0) return;

        if (a == 0) {
            blueprint[i] = b;
        } else if (b == 0) {
            blueprint[j] = a;
        } else {
            var c = Math.min(a, b);
            if (a != c) {
                for (int k = 0, length = blueprint.length; k < length; k++) {
                    if (blueprint[k] == a) blueprint[k] = c;
                }
            } else {
                for (int k = 0, length = blueprint.length; k < length; k++) {
                    if (blueprint[k] == b) blueprint[k] = c;
                }
            }
        }
    }


    public static <T> List<ClusteringEdges> getClusteringEdges(Blueprint<T> blueprint) {
        var tool = new BlueprintTool<>(blueprint);
        if (tool.length() == 0) return List.of();

        var clustering = findClustering(blueprint, false);
        return getClusteringEdges(tool, clustering);
    }

    public static <T> List<ClusteringEdges> getClusteringEdges(Blueprint<T> blueprint, int category) {
        var tool = new BlueprintTool<>(blueprint);
        if (tool.length() == 0) return List.of();

        var clustering = findClustering(blueprint, category, false);
        return getClusteringEdges(tool, clustering);
    }

    private static List<ClusteringEdges> getClusteringEdges(BlueprintTool<?> tool, int[] clustering) {
        var maxGroup = Arrays.stream(clustering).max().orElse(0);
        if (maxGroup == 0) return List.of();

        record Most(int group, int count) {
            Most add(Most other) {
                return new Most(other.group, Most.this.count + other.count);
            }
        }

        // calculate the mode value of clustering groups.
        var most = (int) Arrays.stream(clustering)
          .mapToObj(group -> new Most(group, 1))
          .gather(Gatherer.<Most, HashMap<Integer, Most>, Most>ofSequential(
            HashMap::new,
            (state, element, _) -> {
                state.merge(element.group, element, Most::add);
                return true;
            },
            (state, downstream) -> state.values().forEach(downstream::push)
          )).max(Comparator.comparingInt(Most::count))
          .map(Most::count)
          .orElse(0);

        var src = tool.blueprint();
        var shank = tool.shank();
        var posx = tool.posx();
        var posy = tool.posy();

        var ret = new ArrayList<ClusteringEdges>();

        var area = new int[most];

        for (int g = 0; g < maxGroup; g++) {
            int s = -1;
            int c = ProbeDescription.CATE_UNSET;

            var pointer = 0;
            for (int i = 0, length = clustering.length; i < length; i++) {
                int group = clustering[i];
                if (group == g) {
                    if (s < 0) {
                        s = shank[i];
                        c = src[i];
                    }

                    area[pointer++] = i;
                }
            }

            if (pointer == 1) {
                var x = posx[area[0]];
                var y = posy[area[0]];
                ret.add(ClusteringEdges.point(c, s, x, y));
            } else {
                ret.add(ClusteringEdges.area(c, s, Arrays.copyOfRange(area, 0, pointer), tool));
            }
        }

        return ret;
    }

    public static <T> void fillClusteringEdges(Blueprint<T> blueprint, List<ClusteringEdges> edges) {

    }

    public static <T> void fillClusteringEdges(Blueprint<T> blueprint, List<ClusteringEdges> edges, int category) {
        fillClusteringEdges(blueprint, edges.stream().map(it -> it.withCategory(category)).toList());
    }

    public static <T> void fillClusteringEdges(Blueprint<T> blueprint, ClusteringEdges edge) {

    }

}
