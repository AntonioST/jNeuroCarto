package io.ast.jneurocarto.core.blueprint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.stream.Gatherer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public final class Blueprint<T> {

    final ProbeDescription<T> probe;
    final @Nullable T chmap;

    private static final int[] EMPTY = new int[0];
    final int[] blueprint;
    final int[] shank;
    final int[] posx;
    final int[] posy;
    double dx;
    double dy;

    public Blueprint(ProbeDescription<T> probe) {
        this.probe = probe;
        this.chmap = null;
        blueprint = EMPTY;
        shank = EMPTY;
        posx = EMPTY;
        posy = EMPTY;
    }

    public Blueprint(ProbeDescription<T> probe, T chmap) {
        this.probe = probe;
        this.chmap = chmap;
        var electrodes = probe.allElectrodes(chmap);
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

    public Blueprint(ProbeDescription<T> probe, T chmap, List<ElectrodeDescription> electrodes) {
        this(probe, chmap);
        from(electrodes);
    }

    public Blueprint(Blueprint<T> blueprint) {
        this(blueprint, Objects.requireNonNull(blueprint.chmap, "missing probe"));
    }

    public Blueprint(Blueprint<T> blueprint, T chmap) {
        if (!blueprint.sameChannelmapCode(chmap)) {
            throw new RuntimeException("not the same channelmap code");
        }

        probe = blueprint.probe;
        this.chmap = chmap;
        this.blueprint = blueprint.blueprint.clone();
        shank = blueprint.shank;
        posx = blueprint.posx;
        posy = blueprint.posy;
        dx = blueprint.dx;
        dy = blueprint.dy;
    }

    private static double minDiffSet(int[] x) {
        return Arrays.stream(x)
          .distinct()
          .sorted()
          .boxed()
          .gather(Gatherer.<Integer, int[], Integer>ofSequential(
            () -> new int[]{Integer.MIN_VALUE},
            (state, element, downstream) -> {
                if (state[0] == Integer.MIN_VALUE) {
                    state[0] = element;
                    return true;
                } else {
                    var ret = element - state[0];
                    state[0] = element;
                    return downstream.push(ret);
                }
            }
          )).mapToInt(it -> it)
          .min()
          .orElse(0);
    }

    public ProbeDescription<T> probe() {
        return probe;
    }

    /**
     * {@return total electrode numbers}
     */
    public int size() {
        return blueprint.length;
    }

    /**
     * {@return carried channelmap}.
     */
    public @Nullable T channelmap() {
        return chmap;
    }

    public T newChannelmap() {
        return probe.newChannelmap(Objects.requireNonNull(chmap, "missing channelmap"));
    }

    public List<ElectrodeDescription> electrodes() {
        if (chmap == null) throw new RuntimeException("missing channelmap");
        var electrodes = probe.allElectrodes(chmap);
        applyBlueprint(electrodes);
        return electrodes;
    }

    public Stream<Electrode> stream() {
        return IntStream.range(0, blueprint.length)
          .mapToObj(i -> new Electrode(i, shank[i], posx[i], posy[i], blueprint[i]));
    }

    public Blueprint<T> clear() {
        Arrays.fill(blueprint, ProbeDescription.CATE_UNSET);
        return this;
    }

    public OptionalInt index(int s, int x, int y) {
        for (int i = 0, length = blueprint.length; i < length; i++) {
            if (shank[i] == s && posx[i] == x && posy[i] == y) return OptionalInt.of(i);
        }
        return OptionalInt.empty();
    }

    public OptionalInt index(ElectrodeDescription e) {
        return index(e.s(), e.x(), e.y());
    }

    public Blueprint<T> from(Blueprint<T> blueprint) {
        var length = this.blueprint.length;
        if (blueprint.blueprint.length != length) throw new RuntimeException();
        System.arraycopy(blueprint.blueprint, 0, this.blueprint, 0, length);
        return this;
    }

    public Blueprint<T> from(List<ElectrodeDescription> electrodes) {
        for (var e : electrodes) {
            index(e).ifPresent(i -> blueprint[i] = e.category());
        }
        return this;
    }

    public void applyBlueprint(List<ElectrodeDescription> electrodes) {
        for (var e : electrodes) {
            index(e).ifPresentOrElse(i -> e.category(blueprint[i]),
              () -> e.category(ProbeDescription.CATE_UNSET));
        }
    }

    public void load(Path file) throws IOException {
        if (chmap == null) throw new RuntimeException("missing channelmap");

        from(probe.loadBlueprint(file, chmap));
    }

    public void save(Path file) throws IOException {
        if (chmap == null) throw new RuntimeException("missing channelmap");

        probe.saveBlueprint(file, electrodes());
    }

    public Blueprint<T> set(int category) {
        Arrays.fill(blueprint, category);
        return this;
    }

    public Blueprint<T> setTo(int category, int newCategory) {
        if (category != newCategory) {
            for (int i = 0, length = blueprint.length; i < length; i++) {
                if (blueprint[i] == category) {
                    blueprint[i] = newCategory;
                }
            }
        }
        return this;
    }

    /**
     * @param category   electrode category
     * @param electrodes
     * @return
     */
    public Blueprint<T> set(int category, List<ElectrodeDescription> electrodes) {
        for (var electrode : electrodes) {
            index(electrode).ifPresent(i -> blueprint[i] = category);
        }

        return this;
    }

    public Blueprint<T> set(int category, int index) {
        blueprint[index] = category;
        return this;
    }

    /**
     * @param category electrode category
     * @param index    electrode index array
     * @return
     */
    public Blueprint<T> set(int category, int[] index) {
        for (int i : index) {
            blueprint[i] = category;
        }

        return this;
    }

    /**
     * @param category      electrode category
     * @param electrodeMask
     * @return
     */
    public Blueprint<T> set(int category, boolean[] electrodeMask) {
        if (blueprint.length != electrodeMask.length) throw new RuntimeException();

        for (int i = 0, length = electrodeMask.length; i < length; i++) {
            if (electrodeMask[i]) blueprint[i] = category;
        }

        return this;
    }

    /**
     * @param category electrode category
     * @param pick
     * @return
     */
    public Blueprint<T> set(int category, Predicate<Electrode> pick) {
        stream().filter(pick).forEach(it -> {
            blueprint[it.i()] = category;
        });
        return this;
    }

    public Blueprint<T> unset(int category) {
        return setTo(category, ProbeDescription.CATE_UNSET);
    }

    public Blueprint<T> unset(List<ElectrodeDescription> electrodes) {
        return set(ProbeDescription.CATE_UNSET, electrodes);
    }

    /**
     * @param electrodeIndex electrode index array
     * @return
     */
    public Blueprint<T> unset(int[] electrodeIndex) {
        return set(ProbeDescription.CATE_UNSET, electrodeIndex);
    }

    public Blueprint<T> unset(boolean[] electrodeMask) {
        return set(ProbeDescription.CATE_UNSET, electrodeMask);
    }

    public Blueprint<T> unset(Predicate<Electrode> pick) {
        return set(ProbeDescription.CATE_UNSET, pick);
    }

    public Blueprint<T> merge(List<ElectrodeDescription> electrodes) {
        for (var electrode : electrodes) {
            index(electrode).ifPresent(i -> {
                if (blueprint[i] == ProbeDescription.CATE_UNSET) {
                    blueprint[i] = electrode.category();
                }
            });
        }

        return this;
    }

    public Blueprint<T> merge(Blueprint<T> other) {
        var length = blueprint.length;
        if (length != other.blueprint.length) throw new RuntimeException();
        if (this != other) {
            for (int i = 0; i < length; i++) {
                if (blueprint[i] == ProbeDescription.CATE_UNSET) blueprint[i] = other.blueprint[i];
            }
        }
        return this;
    }

    public boolean same(int @Nullable [] blueprint) {
        return Arrays.equals(this.blueprint, blueprint);
    }

    public boolean same(@Nullable List<ElectrodeDescription> electrodes) {
        if (electrodes == null) return false;
        for (var e : electrodes) {
            var i = index(e.s(), e.x(), e.y()).orElse(-1);
            if (i < 0) return false;
            if (blueprint[i] != e.category()) return false;
        }
        return true;
    }

    public boolean same(@Nullable Blueprint<T> blueprint) {
        if (blueprint == null) return false;
        if (probe.getClass() != blueprint.probe.getClass()) return false;
        if (!sameChannelmapCode(blueprint.chmap)) return false;
        return Arrays.equals(this.blueprint, blueprint.blueprint);
    }

    public boolean sameChannelmapCode(@Nullable Object chmap) {
        return this.chmap != null && chmap != null
               && Objects.equals(probe.channelmapCode(this.chmap), probe.channelmapCode(chmap));
    }
}
