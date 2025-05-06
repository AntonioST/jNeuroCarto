package io.ast.jneurocarto.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.stream.Gatherer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Blueprint<T> {

    private final ProbeDescription<T> probe;
    private final @Nullable T chmap;
    private final List<ElectrodeDescription> electrodes;

    private static final int[] EMPTY_BLUEPRINT = new int[0];
    private final int[] blueprint;

    /**
     * any modification on blueprint, but no sync back to electrodes.
     */
    private boolean modified = false;

    private int[] @Nullable shank;
    private int[] @Nullable posx;
    private int[] @Nullable posy;
    private int dx;
    private int dy;


    public Blueprint(ProbeDescription<T> probe) {
        this.probe = probe;
        this.chmap = null;
        electrodes = List.of();
        blueprint = EMPTY_BLUEPRINT;
    }

    public Blueprint(ProbeDescription<T> probe, T chmap) {
        this.probe = probe;
        this.chmap = chmap;
        electrodes = probe.allElectrodes(chmap);
        blueprint = new int[electrodes.size()];

        shank = new int[blueprint.length];
        posx = new int[blueprint.length];
        posy = new int[blueprint.length];
        for (int i = 0; i < blueprint.length; i++) {
            shank[i] = electrodes.get(i).s();
            posx[i] = electrodes.get(i).x();
            posy[i] = electrodes.get(i).y();
        }

        dx = minDiffSet(posx);
        dy = minDiffSet(posy);
    }

    public Blueprint(Blueprint<T> blueprint) {
        probe = blueprint.probe;
        chmap = blueprint.chmap;
        electrodes = blueprint.electrodes;
        this.blueprint = new int[electrodes.size()];

        if (chmap != null) {
            shank = blueprint.shank;
            posx = blueprint.posx;
            posy = blueprint.posy;
            dx = blueprint.dx;
            dy = blueprint.dy;
        }
    }

    private static int minDiffSet(int[] x) {
        return Arrays.stream(x)
          .distinct()
          .sorted()
          .boxed()
          .gather(Gatherer.<Integer, int[], Integer>ofSequential(
            () -> new int[0],
            (state, element, downstream) -> {
                var ret = element - state[0];
                state[0] = element;
                return downstream.push(ret);
            }
          )).mapToInt(it -> it)
          .min()
          .orElse(0);
    }

    public int size() {
        return blueprint.length;
    }

    public List<ElectrodeDescription> electeodes() {
        if (modified) applyBlueprint();
        return probe.copyElectrodes(electrodes);
    }

    public Blueprint<T> clear() {
        Arrays.fill(blueprint, ProbeDescription.CATE_UNSET);
        modified = true;
        return this;
    }

    public boolean isModified() {
        return modified;
    }

    public OptionalInt indexElectrode(ElectrodeDescription e) {
        int s = e.s();
        int x = e.x();
        int y = e.y();
        for (int i = 0, length = blueprint.length; i < length; i++) {
            if (shank[i] == s && posx[i] == x && posy[i] == y) return OptionalInt.of(i);
        }
        return OptionalInt.empty();
    }

    public int[] indexBlueprint(List<ElectrodeDescription> e) {
        var ret = new int[e.size()];
        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = indexElectrode(e.get(i)).orElse(-1);
        }
        return ret;
    }

    public int[] indexBlueprint(Predicate<ElectrodeDescription> picker) {
        var ret = new int[blueprint.length];
        int size = 0;
        for (int i = 0, length = electrodes.size(); i < length; i++) {
            if (picker.test(electrodes.get(i))) {
                ret[size++] = i;
            }
        }
        return Arrays.copyOfRange(ret, 0, size);
    }


    public boolean[] maskBlueprint(List<ElectrodeDescription> e) {
        var ret = new boolean[blueprint.length];
        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = indexElectrode(e.get(i)).isPresent();
        }
        return ret;
    }

    public boolean[] maskBlueprint(Predicate<ElectrodeDescription> picker) {
        var ret = new boolean[blueprint.length];
        for (int i = 0, length = electrodes.size(); i < length; i++) {
            ret[i] = picker.test(electrodes.get(i));
        }
        return ret;
    }

    public Blueprint<T> setBlueprint(int category) {
        Arrays.fill(blueprint, category);
        modified = true;
        return this;
    }

    public Blueprint<T> setBlueprint(Blueprint<T> blueprint) {
        var length = this.blueprint.length;
        if (blueprint.blueprint.length != length) throw new RuntimeException();
        System.arraycopy(blueprint.blueprint, 0, this.blueprint, 0, length);
        modified = true;
        return this;
    }

    public Blueprint<T> setBlueprint(List<ElectrodeDescription> electrodes) {
        for (var e : electrodes) {
            indexElectrode(e).ifPresent(i -> blueprint[i] = e.category());
        }
        modified = true;
        return this;
    }

    public Blueprint<T> applyBlueprint() {
        applyBlueprint(electrodes);
        modified = false;
        return this;
    }

    public void applyBlueprint(List<ElectrodeDescription> electrodes) {
        for (var e : electrodes) {
            indexElectrode(e).ifPresentOrElse(i -> e.category(blueprint[i]),
              () -> e.category(ProbeDescription.CATE_UNSET));
        }
    }

    public void load(Path file) throws IOException {
        if (chmap == null) throw new RuntimeException("missing channelmap");

        setBlueprint(probe.loadBlueprint(file, chmap));
    }

    public void save(Path file) throws IOException {
        if (chmap == null) throw new RuntimeException("missing channelmap");

        probe.saveBlueprint(file, electeodes());
    }

    public Blueprint<T> set(int category, List<ElectrodeDescription> electrodes) {
        for (var electrode : electrodes) {
            indexElectrode(electrode).ifPresent(i -> blueprint[i] = category);
        }

        modified = true;
        return this;
    }

    public Blueprint<T> set(int category, int[] electrodeIndex) {
        for (int index : electrodeIndex) {
            blueprint[index] = category;
        }

        modified = true;
        return this;
    }

    public Blueprint<T> set(int category, boolean[] electrodeMask) {
        if (blueprint.length != electrodeMask.length) throw new RuntimeException();

        for (int i = 0, length = electrodeMask.length; i < length; i++) {
            if (electrodeMask[i]) blueprint[i] = category;
        }

        modified = true;
        return this;
    }

    public Blueprint<T> set(int category, Predicate<ElectrodeDescription> pick) {
        for (int i = 0, length = blueprint.length; i < length; i++) {
            if (pick.test(electrodes.get(i))) blueprint[i] = category;
        }

        modified = true;
        return this;
    }

    public Blueprint<T> unset(List<ElectrodeDescription> electrodes) {
        return set(ProbeDescription.CATE_UNSET, electrodes);
    }

    public Blueprint<T> unset(int[] electrodeIndex) {
        return set(ProbeDescription.CATE_UNSET, electrodeIndex);
    }

    public Blueprint<T> unset(boolean[] electrodeMask) {
        return set(ProbeDescription.CATE_UNSET, electrodeMask);
    }

    public Blueprint<T> unset(Predicate<ElectrodeDescription> pick) {
        return set(ProbeDescription.CATE_UNSET, pick);
    }

    public Blueprint<T> merge(List<ElectrodeDescription> electrodes) {
        for (var electrode : electrodes) {
            indexElectrode(electrode).ifPresent(i -> {
                if (blueprint[i] == ProbeDescription.CATE_UNSET) {
                    blueprint[i] = electrode.category();
                }
            });
        }

        modified = true;
        return this;
    }

    public Blueprint<T> merge(Blueprint<T> other) {
        var length = blueprint.length;
        if (length != other.blueprint.length) throw new RuntimeException();

        for (int i = 0; i < length; i++) {
            if (blueprint[i] == ProbeDescription.CATE_UNSET) blueprint[i] = other.blueprint[i];
        }

        modified = true;
        return this;
    }

    public int countCategory(int category) {
        var ret = 0;
        for (int c : blueprint) {
            if (c == category) ret++;
        }
        return ret;
    }

    public int countCategory(int category, List<ElectrodeDescription> electrodes) {
        return (int) electrodes.stream()
          .flatMapToInt(e -> indexElectrode(e).stream())
          .filter(i -> blueprint[i] == category)
          .count();
    }

    public int countCategory(int category, int[] electrodeIndex) {
        var ret = 0;
        for (int index : electrodeIndex) {
            if (blueprint[index] == category) ret++;
        }
        return ret;
    }

    public int countCategory(int category, boolean[] electrodeMask) {
        int length = blueprint.length;
        if (length != electrodeMask.length) throw new RuntimeException();

        var ret = 0;
        for (int i = 0; i < length; i++) {
            if (electrodeMask[i] && blueprint[i] == category) ret++;
        }
        return ret;
    }

    public int countCategory(int category, Predicate<ElectrodeDescription> pick) {
        var ret = 0;
        for (int i = 0, length = blueprint.length; i < length; i++) {
            if (pick.test(electrodes.get(i)) && blueprint[i] == category) ret++;
        }
        return ret;
    }

    public boolean[] maskCategory(int category) {
        var ret = new boolean[blueprint.length];
        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = blueprint[i] == category;
        }
        return ret;
    }

    public boolean[] maskCategory(int category, List<ElectrodeDescription> electrodes) {
        var ret = new boolean[blueprint.length];
        for (var electrode : electrodes) {
            indexElectrode(electrode).ifPresent(i -> {
                ret[i] = blueprint[i] == category;
            });
        }
        return ret;
    }

    public boolean[] maskCategory(int category, int[] electrodeIndex) {
        var ret = new boolean[blueprint.length];
        for (int i : electrodeIndex) {
            ret[i] = blueprint[i] == category;
        }
        return ret;
    }

    public boolean[] maskCategory(int category, boolean[] electrodeMask) {
        int length = blueprint.length;
        if (length != electrodeMask.length) throw new RuntimeException();

        var ret = new boolean[length];
        for (int i = 0; i < length; i++) {
            ret[i] = electrodeMask[i] && blueprint[i] == category;
        }
        return ret;
    }

    public boolean[] maskCategory(int category, Predicate<ElectrodeDescription> pick) {
        var ret = new boolean[blueprint.length];
        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = pick.test(electrodes.get(i)) && blueprint[i] == category;
        }
        return ret;
    }
}
