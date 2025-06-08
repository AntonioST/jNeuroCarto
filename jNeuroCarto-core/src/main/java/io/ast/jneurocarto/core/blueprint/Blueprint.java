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

/**
 * Blueprint. It provides more dense data structure to store electrode categories,
 * and efficient on manipulation.
 *
 * @param <T> channelmap type.
 */
@NullMarked
public final class Blueprint<T> {

    /**
     * The probe description of channelmap {@code T}.
     */
    final ProbeDescription<T> probe;

    /**
     * channelmap
     */
    final @Nullable T chmap;

    private static final int[] EMPTY = new int[0];
    final int[] blueprint;
    final int[] shank;
    final int[] posx;
    final int[] posy;
    double dx;
    double dy;

    /**
     * Create an empty blueprint. You cannot do anything with it.
     *
     * @param probe probe description
     */
    public Blueprint(ProbeDescription<T> probe) {
        this.probe = probe;
        this.chmap = null;
        blueprint = EMPTY;
        shank = EMPTY;
        posx = EMPTY;
        posy = EMPTY;
    }

    /**
     * Create a blank blueprint.
     *
     * @param probe probe description
     * @param chmap channelmap
     */
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

    /**
     * Create a blueprint and transfer electrode categories from {@code electrodes}.
     *
     * @param probe      probe description
     * @param chmap      channelmap
     * @param electrodes electrodes
     */
    public Blueprint(ProbeDescription<T> probe, T chmap, List<ElectrodeDescription> electrodes) {
        this(probe, chmap);
        from(electrodes);
    }

    /**
     * Clone a blueprint.
     *
     * @param blueprint blueprint
     */
    public Blueprint(Blueprint<T> blueprint) {
        this(blueprint, Objects.requireNonNull(blueprint.chmap, "missing probe"));
    }

    /**
     * Clone a blueprint and bind to {@code chmap}.
     *
     * @param blueprint blueprint
     * @param chmap     channelmap
     */
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

    /**
     * {@return probe description}
     */
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

    /**
     * Create a new, empty channelmap instance with the same code of {@code chmap}.
     *
     * @return a channelmap instance
     * @throws RuntimeException it is an empty blueprint.
     * @see ProbeDescription#newChannelmap(Object)
     */
    public T newChannelmap() {
        return probe.newChannelmap(Objects.requireNonNull(chmap, "missing channelmap"));
    }

    /**
     * Returns a list of electrodes that carried the categories from the blueprint.
     *
     * @return a list of electrodes
     * @throws RuntimeException it is an empty blueprint.
     * @see #applyBlueprint(List)
     */
    public List<ElectrodeDescription> electrodes() {
        if (chmap == null) throw new RuntimeException("missing channelmap");
        var electrodes = probe.allElectrodes(chmap);
        applyBlueprint(electrodes);
        return electrodes;
    }

    /**
     * {@return a {@link Electrode} stream}
     */
    public Stream<Electrode> stream() {
        return IntStream.range(0, blueprint.length)
            .mapToObj(i -> new Electrode(i, shank[i], posx[i], posy[i], blueprint[i]));
    }

    /**
     * unset all electrodes to {@link ProbeDescription#CATE_UNSET}.
     *
     * @return this, a blank blueprint.
     */
    public Blueprint<T> clear() {
        Arrays.fill(blueprint, ProbeDescription.CATE_UNSET);
        return this;
    }

    /**
     * Give the electrode index based on electrode's position.
     *
     * @param s shank index
     * @param x x position
     * @param y y position
     * @return optional electrode index
     */
    public OptionalInt index(int s, int x, int y) {
        for (int i = 0, length = blueprint.length; i < length; i++) {
            if (shank[i] == s && posx[i] == x && posy[i] == y) return OptionalInt.of(i);
        }
        return OptionalInt.empty();
    }

    /**
     * Give the electrode index of electrode {@code e}.
     *
     * @param e electrode
     * @return optional electrode index
     */
    public OptionalInt index(ElectrodeDescription e) {
        return index(e.s(), e.x(), e.y());
    }

    /**
     * Read the blueprint from another {@code blueprint}.
     *
     * @param blueprint blueprint
     * @return this
     * @throws IllegalArgumentException electrode number mismatch.
     */
    public Blueprint<T> from(Blueprint<T> blueprint) {
        var length = this.blueprint.length;
        if (blueprint.blueprint.length != length) throw new IllegalArgumentException();
        System.arraycopy(blueprint.blueprint, 0, this.blueprint, 0, length);
        return this;
    }

    /**
     * Read the blueprint from {@code electrodes}.
     *
     * @param electrodes list
     * @return this
     */
    public Blueprint<T> from(List<ElectrodeDescription> electrodes) {
        for (var e : electrodes) {
            index(e).ifPresent(i -> blueprint[i] = e.category());
        }
        return this;
    }

    /**
     * Apply the blueprint into {@code electrodes}.
     *
     * @param electrodes electrode list
     */
    public void applyBlueprint(List<ElectrodeDescription> electrodes) {
        for (var e : electrodes) {
            index(e).ifPresentOrElse(i -> e.category(blueprint[i]),
                () -> e.category(ProbeDescription.CATE_UNSET));
        }
    }

    /**
     * Load blueprint file.
     *
     * @param file blueprint file
     * @throws RuntimeException it is an empty blueprint
     * @throws IOException      any io error
     * @see ProbeDescription#loadBlueprint(Path, Object)
     */
    public void load(Path file) throws IOException {
        if (chmap == null) throw new RuntimeException("missing channelmap");

        from(probe.loadBlueprint(file, chmap));
    }

    /**
     * Save blueprint file.
     *
     * @param file blueprint file
     * @throws IOException any io error
     * @see ProbeDescription#save(Path, Object)
     */
    public void save(Path file) throws IOException {
        if (chmap == null) throw new RuntimeException("missing channelmap");

        probe.saveBlueprint(file, electrodes());
    }

    /**
     * Set all electrode to {@code category}.
     *
     * @param category electrode category
     * @return this
     */
    public Blueprint<T> set(int category) {
        Arrays.fill(blueprint, category);
        return this;
    }

    /**
     * Set electrode to {@code category} for specific electrodes subset.
     *
     * @param category   electrode category
     * @param electrodes electrodes set.
     * @return this
     */
    public Blueprint<T> set(int category, List<ElectrodeDescription> electrodes) {
        for (var electrode : electrodes) {
            index(electrode).ifPresent(i -> blueprint[i] = category);
        }

        return this;
    }

    /**
     * Set electrode to {@code category} with given electrode {@code index}.
     *
     * @param category electrode category
     * @param index    electrode index
     * @return this
     */
    public Blueprint<T> set(int category, int index) {
        blueprint[index] = category;
        return this;
    }

    /**
     * Set electrodes to {@code category} with given electrode {@code index}.
     *
     * @param category electrode category
     * @param index    electrode index array
     * @return this
     */
    public Blueprint<T> set(int category, int[] index) {
        for (int i : index) {
            blueprint[i] = category;
        }

        return this;
    }

    /**
     * Set electrodes to {@code category} with given electrode {@code mask}.
     *
     * @param category electrode category
     * @param mask     electrode mask
     * @return this
     * @throws IllegalArgumentException electrode number mismatch.
     */
    public Blueprint<T> set(int category, boolean[] mask) {
        if (blueprint.length != mask.length) throw new IllegalArgumentException();

        for (int i = 0, length = mask.length; i < length; i++) {
            if (mask[i]) blueprint[i] = category;
        }

        return this;
    }

    /**
     * Set electrodes to {@code category} with given electrode {@code mask}.
     *
     * @param category electrode category
     * @param mask     electrode mask
     * @return this
     * @throws IllegalArgumentException electrode number mismatch.
     */
    public Blueprint<T> set(int category, BlueprintMask mask) {
        if (blueprint.length != mask.length()) throw new IllegalArgumentException();
        mask.forEach(i -> blueprint[i] = category);
        return this;
    }

    /**
     * Set electrodes to {@code category} with given condiction.
     *
     * @param category electrode category
     * @param pick     picking condiction
     * @return this
     */
    public Blueprint<T> set(int category, Predicate<Electrode> pick) {
        stream().filter(pick).forEach(it -> {
            blueprint[it.i()] = category;
        });
        return this;
    }

    /**
     * unset all electrode belong to {@code category}
     *
     * @param category electrode category
     * @return this
     */
    public Blueprint<T> unset(int category) {
        return set(category, BlueprintMask.eq(blueprint, ProbeDescription.CATE_UNSET));
    }

    /**
     * Set electrode for specific electrodes subset.
     *
     * @param electrodes electrodes set.
     * @return this
     */
    public Blueprint<T> unset(List<ElectrodeDescription> electrodes) {
        return set(ProbeDescription.CATE_UNSET, electrodes);
    }

    /**
     * unset electrodes with given electrode {@code index}.
     *
     * @param index electrode index array
     * @return this
     */
    public Blueprint<T> unset(int[] index) {
        return set(ProbeDescription.CATE_UNSET, index);
    }

    /**
     * unset electrodes with given electrode {@code mask}.
     *
     * @param mask electrode mask
     * @return this
     */
    public Blueprint<T> unset(boolean[] mask) {
        return set(ProbeDescription.CATE_UNSET, mask);
    }

    /**
     * unset electrodes with given electrode {@code mask}.
     *
     * @param mask electrode mask
     * @return this
     */
    public Blueprint<T> unset(BlueprintMask mask) {
        return set(ProbeDescription.CATE_UNSET, mask);
    }

    /**
     * unset electrodes with given condiction.
     *
     * @param pick pick condiction
     * @return this
     */
    public Blueprint<T> unset(Predicate<Electrode> pick) {
        return set(ProbeDescription.CATE_UNSET, pick);
    }

    /**
     * merge the categories with {@code electrodes}.
     * <br>
     * All electrode belongs to {@link ProbeDescription#CATE_UNSET} will apply
     * the new category from {@code electrodes}.
     *
     * @param electrodes a list of electrodes
     * @return this
     */
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

    /**
     * merge the categories with another blueprint.
     * <br>
     * All electrode belongs to {@link ProbeDescription#CATE_UNSET} will apply
     * the new category from {@code other}.
     *
     * @param other another blueprint
     * @return this
     */
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


    /*public boolean same(int @Nullable [] blueprint) {
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
    }*/

    /**
     * Test does the carried channelmap ({@link #channelmap()}) have the
     * same channelmap code with {@code chmap}.
     *
     * @param chmap another channelmap
     * @return same or not.
     */
    public boolean sameChannelmapCode(@Nullable Object chmap) {
        return this.chmap != null && chmap != null
               && Objects.equals(probe.channelmapCode(this.chmap), probe.channelmapCode(chmap));
    }
}
