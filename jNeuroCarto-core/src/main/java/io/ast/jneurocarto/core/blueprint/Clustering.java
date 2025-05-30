package io.ast.jneurocarto.core.blueprint;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Gatherer;

import io.ast.jneurocarto.core.ProbeDescription;

public record Clustering(int[] clustering) {

    public static Clustering EMPTY = new Clustering(new int[0]);

    public Clustering(int length) {
        this(new int[length]);
    }

    public Clustering(Clustering clustering) {
        this(clustering.clustering.clone());
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

    void set(int i, int group) {
        clustering[i] = group;
    }

    void fill(int group) {
        Arrays.fill(clustering, group);
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

    public int[] indexGroup() {
        var ret = new int[clustering.length];
        var size = 0;
        for (int i = 0, length = clustering.length; i < length; i++) {
            if (clustering[i] > 0) ret[size++] = i;
        }
        return Arrays.copyOfRange(ret, 0, size);
    }

    public int[] indexGroup(int group) {
        var ret = new int[clustering.length];
        var size = 0;
        for (int i = 0, length = clustering.length; i < length; i++) {
            if (clustering[i] == group) ret[size++] = i;
        }
        return Arrays.copyOfRange(ret, 0, size);
    }

    int indexGroup(int group, int[] output) {
        var pointer = 0;
        for (int i = 0, length = clustering.length; i < length; i++) {
            if (clustering[i] == group) {
                output[pointer++] = i;
            }
        }
        return pointer;
    }

    public BlueprintMask maskGroup() {
        return BlueprintMask.gt(clustering, 0);
    }

    public BlueprintMask maskGroup(int group) {
        return BlueprintMask.eq(clustering, group);
    }

    public void removeGroup(int group) {
        for (int i = 0, length = clustering.length; i < length; i++) {
            if (clustering[i] == group) clustering[i] = 0;
        }
    }

    public int[] isolate(int[] blueprint, int group) {
        return isolate(blueprint, group, ProbeDescription.CATE_UNSET);
    }

    public int[] isolate(int[] blueprint, int group, int[] output) {
        return isolate(blueprint, group, output, ProbeDescription.CATE_UNSET);
    }

    public int[] isolate(int[] blueprint, int group, int zero) {
        if (blueprint.length != clustering.length) throw new RuntimeException();
        for (int i = 0, length = clustering.length; i < length; i++) {
            if (clustering[i] != group) blueprint[i] = zero;
        }
        return blueprint;
    }

    public int[] isolate(int[] blueprint, int group, int[] output, int zero) {
        if (blueprint.length != clustering.length) throw new RuntimeException();
        if (output.length != clustering.length) throw new RuntimeException();
        for (int i = 0, length = clustering.length; i < length; i++) {
            output[i] = clustering[i] == group ? blueprint[i] : zero;
        }
        return blueprint;
    }

    public Mode modeGroup() {
        return Arrays.stream(clustering)
          .filter(group -> group > 0)
          .mapToObj(group -> new Mode(group, 1))
          .gather(Gatherer.<Mode, HashMap<Integer, Mode>, Mode>ofSequential(
            HashMap::new,
            Gatherer.Integrator.ofGreedy((state, element, _) -> {
                state.merge(element.group, element, Mode::add);
                return true;
            }),
            (state, downstream) -> state.values().forEach(downstream::push)
          )).max(Comparator.comparingInt(Mode::count))
          .orElse(new Mode(0, groupCount(0)));
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
