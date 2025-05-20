package io.ast.jneurocarto.core.blueprint;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Gatherer;

public record Clustering(int[] clustering) {

    public static Clustering EMPTY = new Clustering(new int[0]);

    Clustering(int length) {
        this(new int[length]);
    }

    public record Mode(int group, int count) {
        Mode add(Mode other) {
            return new Mode(other.group, Mode.this.count + other.count);
        }
    }

    public int length() {
        return clustering.length;
    }

    public int get(int i) {
        return clustering[i];
    }

    public int groupNumber() {
        return (int) Arrays.stream(clustering)
          .filter(group -> group > 0)
          .distinct()
          .count();
    }

    public int[] groups() {
        return Arrays.stream(clustering)
          .filter(group -> group > 0)
          .distinct()
          .toArray();
    }

    public int groupCount(int group) {
        return (int) Arrays.stream(clustering)
          .filter(it -> it == group)
          .count();
    }

    public Mode modeGroup() {
        return Arrays.stream(clustering)
          .filter(group -> group > 0)
          .mapToObj(group -> new Mode(group, 1))
          .gather(Gatherer.<Mode, HashMap<Integer, Mode>, Mode>ofSequential(
            HashMap::new,
            (state, element, _) -> {
                state.merge(element.group, element, Mode::add);
                return true;
            },
            (state, downstream) -> state.values().forEach(downstream::push)
          )).max(Comparator.comparingInt(Mode::count))
          .orElse(new Mode(0, groupCount(0)));
    }

    void set(int i, int group) {
        clustering[i] = group;
    }

    void fill(int group) {
        Arrays.fill(clustering, group);
    }

    int indexOfGroup(int group, int[] output) {
        var pointer = 0;
        for (int i = 0, length = clustering.length; i < length; i++) {
            if (clustering[i] == group) {
                output[pointer++] = i;
            }
        }
        return pointer;
    }

    int unionClusteringGroup(int i, int j) {
        if (i == j) return clustering[i];

        var a = clustering[i];
        var b = clustering[j];
        if (a == 0 && b == 0) return 0;

        if (a == 0) {
            clustering[i] = b;
            return b;
        } else if (b == 0) {
            clustering[j] = a;
            return a;
        } else {
            var c = Math.min(a, b);
            if (a != c) {
                for (int k = 0, length = clustering.length; k < length; k++) {
                    if (clustering[k] == a) clustering[k] = c;
                }
            } else {
                for (int k = 0, length = clustering.length; k < length; k++) {
                    if (clustering[k] == b) clustering[k] = c;
                }
            }
            return c;
        }
    }
}
