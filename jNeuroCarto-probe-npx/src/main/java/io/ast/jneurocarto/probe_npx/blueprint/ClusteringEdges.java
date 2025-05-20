package io.ast.jneurocarto.probe_npx.blueprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Gatherer;

import io.ast.jneurocarto.core.BlueprintTool;

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
    record Corner(int x, int y, int corner) {
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

    public static ClusteringEdges point(int c, int s, int x, int y) {
        return new ClusteringEdges(c, s, List.of(
          new ClusteringEdges.Corner(x, y, 1),
          new ClusteringEdges.Corner(x, y, 3),
          new ClusteringEdges.Corner(x, y, 5),
          new ClusteringEdges.Corner(x, y, 7)
        ));
    }

    public static ClusteringEdges area(int c, int s, int[] index, BlueprintTool<?> tool) {
        return new ClusteringEdges(c, s, walkArea(index, tool));
    }

    private static List<Corner> walkArea(int[] index, BlueprintTool<?> tool) {
        if (index.length == 0) throw new IllegalArgumentException();

        var posx = tool.posx();
        var posy = tool.posy();

        var start = Arrays.stream(index)
          .mapToObj(i -> new Index(i, posx[i], posy[i]))
          .gather(minOn(Index::x))
          .gather(minOn(Index::y))
          .findFirst()
          .get();

        var ret = new ArrayList<Corner>();

        // * ?
        // i * ?
        //   ? ?
        int i = start.i;
        int d = 0; // right
        int x = start.x;
        int y = start.y;
        ret.add(new Corner(x, y, 5));

        while (!(start.i == i && d == 6)) {
            if (indexOf(index, i) < 0) throw new RuntimeException();

            x = posx[index[i]];
            y = posy[index[i]];

            loop:
            for (var actions : getAction(d)) {
                switch (actions) {
                case Turn(int action, int corner) -> {
                    var j = Surroundings.surrounding(tool, index[i], action);
                    if (j >= 0) { // is index[i]+action located in probe?
                        var k = indexOf(index, j);
                        if (k >= 0) { // is index[i]+action located in index?
                            ret.add(new Corner(x, y, corner));
                            i = k;
                            d = action;
                            break loop;
                        }
                    }
                }
                case Back(int[] corners, int action) -> {
                    for (var corner : corners) {
                        ret.add(new Corner(x, y, corner));
                    }
                    d = action;
                    break loop;
                }
                }
            }
        }

        return ret;
    }

    private static int indexOf(int[] array, int value) {
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] == value) return i;
        }
        return -1;
    }

    private record Index(int i, int x, int y) {
    }

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

    private record WalkAction(int direction, List<FollowAction> actions) {
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

    private static List<FollowAction> getAction(int direction) {
        for (int i = 0, length = ACTIONS.length; i < length; i++) {
            if (ACTIONS[i].direction == direction) return ACTIONS[i].actions;
        }
        throw new RuntimeException();
    }


}
