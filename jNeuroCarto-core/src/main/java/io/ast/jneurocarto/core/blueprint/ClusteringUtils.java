package io.ast.jneurocarto.core.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.stream.Gatherer;

final class ClusteringUtils {
    static ClusteringEdges pointClustering(int c, int s, int x, int y) {
        return new ClusteringEdges(c, s, List.of(
            new ClusteringEdges.Corner(x, y, 1),
            new ClusteringEdges.Corner(x, y, 3),
            new ClusteringEdges.Corner(x, y, 5),
            new ClusteringEdges.Corner(x, y, 7)
        ));
    }

    static ClusteringEdges areaClustering(BlueprintToolkit<?> toolkit, int c, int s, BlueprintMask mask) {
        return new ClusteringEdges(c, s, walkClusteringArea(toolkit, mask));
    }

    private record Index(int i, int x, int y) {
        private static Gatherer<Index, ?, Index> minOn(ToIntFunction<Index> mapper) {
            return Gatherer.<Index, List<Index>, Index>ofSequential(
                ArrayList::new,
                Gatherer.Integrator.ofGreedy((state, element, _) -> {
                    if (!state.isEmpty()) {
                        var i1 = mapper.applyAsInt(element);
                        var i0 = mapper.applyAsInt(state.getFirst());
                        if (i1 < i0) {
                            state.clear();
                        } else if (i0 < i1) {
                            return true;
                        }
                    }
                    state.add(element);
                    return true;
                }),
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

    /// corner code:
    /// ```text
    /// 3 2 1
    /// 4 8 0
    /// 5 6 7
    ///```
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
            new Back(new int[]{7, 1}, 2)
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
            new Back(new int[]{5, 7}, 0)
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
            new Back(new int[]{3, 5}, 6)
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
            new Back(new int[]{1, 3}, 4)
        )),
    };

    private static List<ClusteringEdges.Corner> walkClusteringArea(BlueprintToolkit<?> toolkit, BlueprintMask mask) {
        if (mask.count() == 0) throw new IllegalArgumentException();

        var posx = toolkit.posx();
        var posy = toolkit.posy();

        var start = mask.stream()
            .mapToObj(i -> new Index(i, posx[i], posy[i]))
            .gather(Index.minOn(Index::y))
            .gather(Index.minOn(Index::x))
            .findFirst()
            .get();

        var ret = new ArrayList<ClusteringEdges.Corner>();

        //
        // i j ?
        //   ? ?
        int i = start.i;
        int d = 0; // right
        int x = start.x;
        int y = start.y;
        ret.add(new ClusteringEdges.Corner(x, y, 5));

        var ii = i;
        while (!(start.i == i && d == 6)) {
            x = posx[i];
            y = posy[i];

            loop:
            for (var actions : WalkAction.getAction(d)) {
                switch (actions) {
                case Turn(int action, int corner) -> {
                    var j = toolkit.surrounding(i, action);
                    if (j >= 0 && mask.get(j)) {
                        if (i != ii) ret.add(new ClusteringEdges.Corner(x, y, corner));
                        i = j;
                        d = action;
                        break loop;
                    }
                }
                case Back(int[] corners, int action) -> {
                    for (var corner : corners) {
                        ret.add(new ClusteringEdges.Corner(x, y, corner));
                    }

                    ii = i;
                    d = action;
                    break loop;
                }
                }
            }
        }

        return ret;
    }
}
