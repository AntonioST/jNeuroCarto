package io.ast.jneurocarto.core.blueprint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.numpy.FlatIntArray;
import io.ast.jneurocarto.core.numpy.Numpy;
import io.ast.jneurocarto.core.numpy.UnsupportedNumpyDataFormatException;

import static java.nio.file.StandardOpenOption.*;

/// A wrapper over [Blueprint] but providing more function to
/// manipulate the blueprint.
///
/// ### Limitation
///
/// * It assumed the electrodes in a channelmap are lied on grids.
///
/// @param <T> channelmap type.
@NullMarked
public class BlueprintToolkit<T> {

    /**
     * Wrapped blueprint.
     */
    protected final Blueprint<T> blueprint;

    /**
     * warp {@link BlueprintToolkit} onto {@code blueprint}.
     *
     * @param blueprint blueprint
     */
    public BlueprintToolkit(Blueprint<T> blueprint) {
        this.blueprint = blueprint;
    }

    /**
     * Create a new wrapper over a cloned wrapped blueprint.
     *
     * @return blueprint toolkit.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public BlueprintToolkit<T> clone() {
        return new BlueprintToolkit<>(new Blueprint<>(blueprint));
    }

    /*========*
     * getter *
     *========*/

    /**
     * {@return probe description}
     *
     * @see Blueprint#probe()
     */
    public ProbeDescription<T> probe() {
        return blueprint.probe;
    }

    /**
     * {@return carried channelmap}.
     *
     * @see Blueprint#channelmap()
     */
    public @Nullable T channelmap() {
        return blueprint.channelmap();
    }

    /**
     * Returns a list of electrodes that carried the categories from the blueprint.
     *
     * @return a list of electrodes
     * @throws RuntimeException it is an empty blueprint.
     * @see Blueprint#electrodes()
     */
    public final List<ElectrodeDescription> electrodes() {
        return blueprint.electrodes();
    }

    /**
     * {@return a {@link Electrode} stream}
     *
     * @see Blueprint#stream()
     */
    public Stream<Electrode> stream() {
        return blueprint.stream();
    }

    /**
     * {@return total electrode numbers}
     *
     * @see Blueprint#size()
     */
    public final int length() {
        return blueprint.shank.length;
    }

    /**
     * Shank index for each electrode.
     * <br>
     * Do not modify the content of the array.
     *
     * @return Shank index array.
     */
    public final int[] shank() {
        return blueprint.shank;
    }

    /**
     * {@return number of shanks}
     */
    public final int nShank() {
        if (probe() instanceof DummyProbe dummy) {
            return dummy.nShanks;
        }
        return (int) Arrays.stream(shank()).distinct().count();
    }

    /**
     * X position for each electrode.
     * <br>
     * Do not modify the content of the array.
     *
     * @return x position array.
     */
    public final int[] posx() {
        return blueprint.posx;
    }

    /**
     * Y position for each electrode.
     * <br>
     * Do not modify the content of the array.
     *
     * @return y position array.
     */
    public final int[] posy() {
        return blueprint.posy;
    }

    /**
     * {@return the minimal distance between columns}
     */
    public final double dx() {
        return blueprint.dx;
    }

    /**
     * {@return the minimal distance between rows}
     */
    public final double dy() {
        return blueprint.dy;
    }

    /**
     * Get the category value with given electrode index.
     *
     * @param index electrode index
     * @return category value
     */
    public final int category(int index) {
        return blueprint.blueprint[index];
    }

    /*=================*
     * blueprint array *
     *=================*/

    /**
     * {@return wrapped {@link Blueprint}}
     */
    public final Blueprint<T> blueprint() {
        return blueprint;
    }

    /**
     * The blueprint category array of the wrapped {@link Blueprint}.
     *
     * @return blueprint int array.
     */
    protected final int[] ref() {
        return blueprint.blueprint;
    }

    /**
     * Create a blank blueprint array.
     *
     * @return blueprint int array.
     */
    public final int[] empty() {
        var ret = new int[length()];
        Arrays.fill(ret, ProbeDescription.CATE_UNSET);
        return ret;
    }

    /**
     * pick electrodes (category applied) with given electrode index
     *
     * @param index electrode index array.
     * @return electrode list.
     * @throws IndexOutOfBoundsException electrode index over range.
     * @see #pick(List, int[])
     */
    public final List<ElectrodeDescription> pick(int[] index) {
        return pick(electrodes(), index);
    }

    /**
     * pick electrodes (category applied) with given electrode index
     *
     * @param index  electrode index array.
     * @param offset initial index
     * @param length length
     * @return electrode list.
     * @throws IndexOutOfBoundsException electrode index over range.
     * @see #pick(List, int[], int, int)
     */
    public final List<ElectrodeDescription> pick(int[] index, int offset, int length) {
        return pick(electrodes(), index, offset, length);
    }

    /**
     * pick electrodes (category applied) with given electrode mask
     *
     * @param mask mask.
     * @return electrode list.
     * @throws IllegalArgumentException {@code mask} length mismatch.
     * @see #pick(List, BlueprintMask)
     */
    public final List<ElectrodeDescription> pick(BlueprintMask mask) {
        return pick(electrodes(), mask);
    }

    /**
     * pick electrodes with given electrode index
     *
     * @param electrodes electrode list.
     * @param index      electrode index array.
     * @return electrode list.
     * @throws IllegalArgumentException  length mismatch.
     * @throws IndexOutOfBoundsException electrode index over range.
     * @see #pick(int[])
     */
    public final List<ElectrodeDescription> pick(List<ElectrodeDescription> electrodes, int[] index) {
        if (length() != electrodes.size()) throw new IllegalArgumentException();
        return Arrays.stream(index)
            .mapToObj(electrodes::get)
            .toList();
    }

    /**
     * pick electrodes with given electrode index
     *
     * @param electrodes electrode list.
     * @param index      electrode index array.
     * @param offset     initial index
     * @param length     length
     * @return electrode list.
     * @throws IllegalArgumentException  length mismatch.
     * @throws IndexOutOfBoundsException electrode index over range.
     * @see #pick(int[], int, int)
     */
    public final List<ElectrodeDescription> pick(List<ElectrodeDescription> electrodes, int[] index, int offset, int length) {
        if (length() != electrodes.size()) throw new IllegalArgumentException();
        return IntStream.range(0, length)
            .map(i -> index[i + offset])
            .mapToObj(electrodes::get)
            .toList();
    }

    /**
     * pick electrodes with given electrode mask
     *
     * @param electrodes electrode list.
     * @param mask       mask.
     * @return electrode list.
     * @throws IllegalArgumentException {@code electrode} or {@code mask} length mismatch.
     * @see #pick(BlueprintMask)
     */
    public final List<ElectrodeDescription> pick(List<ElectrodeDescription> electrodes, BlueprintMask mask) {
        var length = length();
        if (length != mask.length()) throw new IllegalArgumentException();
        if (length != electrodes.size()) throw new IllegalArgumentException();
        return mask.stream().mapToObj(electrodes::get).toList();
    }

    /**
     * copy categories value from {@code electrode}.
     *
     * @param electrode electrode list
     * @throws IllegalArgumentException length mismatch.
     * @see #from(int[], List)
     */
    public final void from(List<ElectrodeDescription> electrode) {
        from(blueprint.blueprint, electrode);
    }

    /**
     * copy categories value from {@code electrode} into {@code blueprint}.
     *
     * @param blueprint blueprint int array
     * @param electrode electrode list
     * @throws IllegalArgumentException length mismatch.
     * @see #from(List)
     */
    public final void from(int[] blueprint, List<ElectrodeDescription> electrode) {
        if (length() != blueprint.length) throw new IllegalArgumentException();
        for (var e : electrode) {
            var i = index(e.s(), e.x(), e.y());
            if (i >= 0) {
                blueprint[i] = e.category();
            }
        }
    }

    /**
     * copy value from {@code blueprint}.
     *
     * @param blueprint blueprint int array
     * @throws IllegalArgumentException length mismatch.
     */
    public final void from(int[] blueprint) {
        var dst = this.blueprint.blueprint;
        if (blueprint.length != dst.length) throw new IllegalArgumentException();
        if (dst != blueprint) {
            System.arraycopy(blueprint, 0, dst, 0, dst.length);
        }
    }

    /**
     * copy value from {@code blueprint} with a mask.
     * The unmasked categories keep unchange.
     *
     * @param blueprint blueprint int array
     * @param mask      mask
     * @throws IllegalArgumentException length mismatch.
     * @see BlueprintMask#where(int[], int[])
     */
    public final void from(int[] blueprint, BlueprintMask mask) {
        var output = this.blueprint.blueprint;
        mask.where(output, blueprint);
    }

    /**
     * copy value from {@code blueprint} with a mask.
     * The unmasked categories keep unchange.
     *
     * @param writeMask mask on this
     * @param blueprint blueprint int array
     * @param readMask  mask on {@code blueprint}
     * @throws IllegalArgumentException length mismatch.
     * @see BlueprintMask#where(int[], int[], BlueprintMask)
     */
    public final void from(BlueprintMask writeMask, int[] blueprint, BlueprintMask readMask) {
        var output = this.blueprint.blueprint;
        writeMask.where(output, blueprint, readMask);
    }

    /**
     * copy blueprint array from {@code blueprint}.
     *
     * @param blueprint blueprint
     * @throws IllegalArgumentException length mismatch.
     * @see #from(int[])
     */
    public final void from(Blueprint<T> blueprint) {
        if (blueprint != this.blueprint) {
            from(blueprint.blueprint);
        }
    }

    /**
     * copy value from {@code blueprint} with a mask.
     * The unmasked categories keep unchange.
     *
     * @param blueprint blueprint
     * @param mask      mask
     * @throws IllegalArgumentException length mismatch.
     * @see #from(int[], BlueprintMask)
     */
    public final void from(Blueprint<T> blueprint, BlueprintMask mask) {
        if (blueprint != this.blueprint) {
            from(blueprint.blueprint, mask);
        }
    }

    /**
     * copy value from {@code blueprint} with a mask.
     * The unmasked categories keep unchange.
     *
     * @param writeMask mask on this
     * @param blueprint blueprint
     * @param readMask  mask on {@code blueprint}
     * @throws IllegalArgumentException length mismatch.
     * @see #from(BlueprintMask, int[], BlueprintMask)
     */
    public final void from(BlueprintMask writeMask, Blueprint<T> blueprint, BlueprintMask readMask) {
        if (blueprint != this.blueprint) {
            from(writeMask, blueprint.blueprint, readMask);
        }
    }

    /**
     * copy blueprint array from {@code blueprint}.
     *
     * @param blueprint toolkit
     * @throws IllegalArgumentException length mismatch.
     * @see #from(int[])
     */
    public final void from(BlueprintToolkit<T> blueprint) {
        from(blueprint.blueprint);
    }

    /**
     * copy value from {@code blueprint} with a mask.
     * The unmasked categories keep unchange.
     *
     * @param blueprint toolkit
     * @param mask      mask
     * @throws IllegalArgumentException length mismatch.
     * @see #from(int[], BlueprintMask)
     */
    public final void from(BlueprintToolkit<T> blueprint, BlueprintMask mask) {
        from(blueprint.blueprint, mask);
    }

    /**
     * copy value from {@code blueprint} with a mask.
     * The unmasked categories keep unchange.
     *
     * @param writeMask mask on this
     * @param blueprint toolkit
     * @param readMask  mask on {@code blueprint}
     * @throws IllegalArgumentException length mismatch.
     * @see #from(BlueprintMask, int[], BlueprintMask)
     */
    public final void from(BlueprintMask writeMask, BlueprintToolkit<T> blueprint, BlueprintMask readMask) {
        from(writeMask, blueprint.blueprint, readMask);
    }

    /**
     * unset all electrodes to {@link ProbeDescription#CATE_UNSET}.
     *
     * @see Blueprint#clear()
     */
    public final void clear() {
        blueprint.clear();
    }

    /**
     * Set all electrode to {@code category}.
     *
     * @param category electrode category
     * @see Blueprint#set(int)
     */
    public final void set(int category) {
        blueprint.set(category);
    }

    /**
     * Set electrode to {@code category} for specific electrodes subset.
     *
     * @param category   electrode category
     * @param electrodes electrodes set.
     * @see Blueprint#set(int, List)
     */
    public final void set(int category, List<ElectrodeDescription> electrodes) {
        blueprint.set(category, electrodes);
    }

    /**
     * Set electrode to {@code category} with given electrode {@code index}.
     *
     * @param category electrode category
     * @param index    electrode index
     * @see Blueprint#set(int, int)
     */
    public final void set(int category, int index) {
        blueprint.set(category, index);
    }

    /**
     * Set electrode to {@code category} with given electrode {@code index}.
     *
     * @param category electrode category
     * @param index    electrode index
     * @see Blueprint#set(int, int[])
     */
    public final void set(int category, int[] index) {
        blueprint.set(category, index);
    }

    /**
     * Set electrode to {@code category} with given electrode {@code mask}.
     *
     * @param category electrode category
     * @param mask     mask
     * @see Blueprint#set(int, BlueprintMask)
     */
    public final void set(int category, BlueprintMask mask) {
        blueprint.set(category, mask.asBooleanMask());
    }

    /**
     * Set electrodes to {@code category} with given condiction.
     *
     * @param category electrode category
     * @param pick     picking condiction
     * @see Blueprint#set(int, Predicate)
     */
    public final void set(int category, Predicate<Electrode> pick) {
        blueprint.set(category, pick);
    }

    /**
     * unset all electrode belong to {@code category}
     *
     * @param category electrode category
     * @see Blueprint#unset(int)
     */
    public final void unset(int category) {
        blueprint.unset(category);
    }

    /**
     * Set electrode for specific electrodes subset.
     *
     * @param electrodes electrodes set.
     * @see Blueprint#unset(List)
     */
    public final void unset(List<ElectrodeDescription> electrodes) {
        blueprint.unset(electrodes);
    }

    /**
     * unset electrodes with given electrode {@code index}.
     *
     * @param index electrode index array
     * @see Blueprint#unset(int[])
     */
    public final void unset(int[] index) {
        blueprint.unset(index);
    }

    /**
     * unset electrodes with given electrode {@code mask}.
     *
     * @param mask electrode mask
     * @see Blueprint#unset(BlueprintMask)
     */
    public final void unset(BlueprintMask mask) {
        blueprint.unset(mask);
    }

    /**
     * unset electrodes with given condiction.
     *
     * @param pick pick condiction
     * @see Blueprint#unset(Predicate)
     */
    public final void unset(Predicate<Electrode> pick) {
        blueprint.unset(pick);
    }

    /**
     * merge the categories with another blueprint.
     * <br>
     * All electrode belongs to {@link ProbeDescription#CATE_UNSET} will apply
     * the new category from {@code electrodes}.
     *
     * @param blueprint another blueprint int array.
     * @throws IllegalArgumentException length mismatch.
     * @see #merge(Blueprint)
     * @see #merge(BlueprintToolkit)
     */
    public final void merge(int[] blueprint) {
        var dst = this.blueprint.blueprint;
        if (blueprint.length != dst.length) throw new RuntimeException();
        if (dst != blueprint) {
            for (int i = 0, length = dst.length; i < length; i++) {
                if (dst[i] == ProbeDescription.CATE_UNSET) dst[i] = blueprint[i];
            }
        }
    }

    /**
     * merge the categories with another blueprint.
     * <br>
     * All electrode belongs to {@link ProbeDescription#CATE_UNSET} will apply
     * the new category from {@code electrodes}.
     *
     * @param blueprint another blueprint
     * @throws IllegalArgumentException length mismatch.
     * @see #merge(int[])
     * @see #merge(BlueprintToolkit)
     */
    public final void merge(Blueprint<T> blueprint) {
        if (blueprint != this.blueprint) {
            merge(blueprint.blueprint);
        }
    }

    /**
     * merge the categories with another blueprint.
     * <br>
     * All electrode belongs to {@link ProbeDescription#CATE_UNSET} will apply
     * the new category from {@code electrodes}.
     *
     * @param blueprint another blueprint toolkit
     * @throws IllegalArgumentException length mismatch.
     * @see #merge(int[])
     * @see #merge(Blueprint)
     */
    public final void merge(BlueprintToolkit<T> blueprint) {
        merge(blueprint.blueprint);
    }

    /**
     * Apply the blueprint into {@code electrodes}.
     *
     * @param blueprint output blueprint int array
     * @throws IllegalArgumentException length mismatch.
     */
    public final void apply(int[] blueprint) {
        var src = this.blueprint.blueprint;
        var length = src.length;
        if (length != blueprint.length) throw new IllegalArgumentException();
        if (src != blueprint) {
            System.arraycopy(src, 0, blueprint, 0, length);
        }
    }

    /**
     * Apply the blueprint into {@code electrodes}.
     *
     * @param electrodes electrode list
     * @throws IllegalArgumentException length mismatch.
     * @see Blueprint#applyBlueprint(List)
     */
    public final void apply(List<ElectrodeDescription> electrodes) {
        blueprint.applyBlueprint(electrodes);
    }

    /**
     * Apply the blueprint into {@code image}.
     *
     * @param image 2d flatten int array
     * @throws IllegalArgumentException wrong dimension or length mismatch.
     * @see #apply(int[])
     */
    public final void apply(FlatIntArray image) {
        if (image.ndim() != 2) throw new IllegalArgumentException("not a 2d image");
        apply(image.array());
    }

    /**
     * (debug) print blueprint to sout
     *
     * @see #print(int[])
     */
    public final void print() {
        print(blueprint.blueprint);
    }

    /**
     * (debug) print blueprint to {@link String}.
     *
     * @return printed blueprint string
     * @see #toString(int[])
     */
    @Override
    public final String toString() {
        return toString(blueprint.blueprint);
    }

    /**
     * (debug) print {@code blueprint} to sout.
     *
     * @param blueprint blueprint int array
     * @see #print(int[], Appendable)
     */
    public final void print(int[] blueprint) {
        try {
            print(blueprint, System.out);
        } catch (IOException e) {
        }
    }

    /**
     * (debug) print blueprint mask to sout.
     *
     * @param blueprint blueprint mask
     * @see #print(int[], Appendable)
     */
    public final void print(BlueprintMask blueprint) {
        var ret = new int[blueprint.length()];
        blueprint.fill(ret, 1);
        try {
            print(ret, System.out);
        } catch (IOException e) {
        }
    }

    /**
     * (debug) print blueprint mask to {@link String}.
     *
     * @param blueprint blueprint int array
     * @see #print(int[], Appendable)
     */
    public final String toString(int[] blueprint) {
        var sb = new StringBuilder();
        try {
            print(blueprint, sb);
        } catch (IOException e) {
        }
        return sb.toString();
    }

    /**
     * (debug) print blueprint mask to {@link String}.
     *
     * @param blueprint blueprint mask
     * @see #print(int[], Appendable)
     */
    public final String toString(BlueprintMask blueprint) {
        var ret = new int[blueprint.length()];
        blueprint.fill(ret, 1);
        var sb = new StringBuilder();
        try {
            print(ret, sb);
        } catch (IOException e) {
        }
        return sb.toString();
    }

    /**
     * (debug) print blueprint mask.
     *
     * @param blueprint blueprint int array
     */
    public void print(int[] blueprint, Appendable out) throws IOException {
        if (length() != blueprint.length) throw new RuntimeException();

        var shanks = Arrays.stream(shank()).distinct().sorted().toArray();
        var posx = Arrays.stream(posx()).distinct().sorted().toArray();
        var posy = Arrays.stream(posy()).distinct().sorted().toArray();
        var maxCate = Arrays.stream(blueprint).max().orElse(0);
        var format = maxCate >= 10 ? "%2d" : "%d";
        var empty = maxCate >= 10 ? "  " : " ";

        for (int yi = 0, ny = posy.length; yi < ny; yi++) {
            int y = posy[yi];
            for (int si = 0, ns = shanks.length; si < ns; si++) {
                int s = shanks[si];
                if (si > 0) out.append('|');

                var x0 = getX0OnShank(s);
                if (x0.isEmpty()) continue;
                var x0i = Arrays.binarySearch(posx, x0.getAsInt());
                assert x0i >= 0;

                for (int xi = x0i, nx = posx.length; xi < nx; xi++) {
                    int x = posx[xi];
                    int i = index(s, x, y);
                    if (i >= 0) {
                        if (xi - x0i > 0) out.append(' ');
                        out.append(format.formatted(blueprint[i]));
                    }
                }
            }
            out.append('\n');
        }
    }

    private OptionalInt getX0OnShank(int s) {
        var shank = shank();
        var posx = posx();

        return IntStream.range(0, shank.length)
            .filter(i -> shank[i] == s)
            .map(i -> posx[i])
            .min();
    }

    /*===========================*
     * blueprint-like data array *
     *===========================*/

    /**
     * Give the electrode index of electrode {@code e}.
     *
     * @param s shank index
     * @param x x position
     * @param y y position
     * @return electrode index. {@code -1} if not found.
     */
    public int index(int s, int x, int y) {
        if (probe() instanceof DummyProbe dummy) {
            if (0 <= s && s < dummy.nShanks && 0 <= x && x < dummy.nColumns && 0 <= y && y < dummy.nRows) {
                return s * dummy.nColumns * dummy.nRows + y * dummy.nColumns + x;
            }
        } else {
            var shank = shank();
            var posx = posx();
            var posy = posy();
            for (int i = 0, length = length(); i < length; i++) {
                // posy has move unique value in general, so we test first for shortcut fail earlier.
                if (posy[i] == y && shank[i] == s && posx[i] == x) return i;
            }
        }
        return -1;
    }

    /**
     * Get electrode index from selected electrodes in carried channelmap.
     *
     * @return electrode index array
     * @throws RuntimeException missing channelmap
     * @see #index(Object)
     */
    public int[] index() {
        return index(Objects.requireNonNull(blueprint.chmap, "missing channelmap"));
    }

    /**
     * Get electrode index from selected electrodes in {@code chmap}.
     *
     * @param chmap a channelmap
     * @return electrode index array
     * @see ProbeDescription#allChannels(Object)
     * @see #index(int, int, int)
     */
    public int[] index(T chmap) {
        return index(blueprint.probe.allChannels(chmap));
    }

    /**
     * get the electrode index with given electrode list.
     *
     * @param electrode electrode list
     * @return electrode index array
     * @see #index(int, int, int)
     */
    public int[] index(List<ElectrodeDescription> electrode) {
        var ret = new int[electrode.size()];
        for (int i = 0, length = ret.length; i < length; i++) {
            var t = electrode.get(i);
            ret[i] = index(t.s(), t.x(), t.y());
        }
        return ret;
    }

    /**
     * Get the electrode index for those electrode belong to {@code category}.
     *
     * @param blueprint blueprint int array
     * @param category  electrode category
     * @return electrode index array
     * @throws IllegalArgumentException wrong dimension or length mismatch.
     */
    public int[] index(int[] blueprint, int category) {
        if (length() != blueprint.length) throw new IllegalArgumentException();

        var ret = new int[length()];
        int size = 0;
        for (int i = 0, length = length(); i < length; i++) {
            if (blueprint[i] == category) {
                ret[size++] = i;
            }
        }
        return Arrays.copyOfRange(ret, 0, size);
    }

    /**
     * Get the electrode index for those pass the given condiction.
     *
     * @param picker electrode picker
     * @return electrode index array
     */
    public int[] index(Predicate<Electrode> picker) {
        return filter(picker).mapToInt(Electrode::i).toArray();
    }

    /**
     * Returns a blueprint mask with all {@code true} or all {@code false}.
     *
     * @param value initial value.
     * @return a blueprint mask.
     */
    public BlueprintMask mask(boolean value) {
        var mask = new BlueprintMask(length());
        if (value) mask.inot();
        return mask;
    }

    /**
     * Get a blueprint mask on selected electrodes in carried channelmap.
     *
     * @return a blueprint mask.
     * @see ProbeDescription#allChannels(Object)
     * @see #mask(Object)
     */
    public BlueprintMask mask() {
        return mask(Objects.requireNonNull(blueprint.chmap, "missing channelmap"));
    }

    /**
     * Get a blueprint mask on selected electrodes in {@code chmap}.
     *
     * @param chmap a channelmap
     * @return a blueprint mask.
     * @see ProbeDescription#allChannels(Object)
     */
    public BlueprintMask mask(T chmap) {
        return mask(blueprint.probe.allChannels(chmap));
    }

    /**
     * Get a blueprint mask with given electrode list.
     *
     * @param electrodes electrode list
     * @return a blueprint mask.
     */
    public BlueprintMask mask(List<ElectrodeDescription> electrodes) {
        var ret = new BlueprintMask(length());
        for (var e : electrodes) {
            var j = index(e.s(), e.x(), e.y());
            if (j >= 0) ret.set(j);
        }
        return ret;
    }

    /**
     * Get a blueprint mask with given condiction.
     *
     * @param picker electrode picker
     * @return a blueprint mask.
     */
    public final BlueprintMask mask(Predicate<Electrode> picker) {
        var ret = new BlueprintMask(length());
        filter(picker).mapToInt(Electrode::i).forEach(ret::set);
        return ret;
    }

    /**
     * Get a blueprint mask with given electrode index array.
     * {@code -1} are ignored.
     *
     * @param index electrode index array
     * @return a blueprint mask.
     */
    public final BlueprintMask mask(int[] index) {
        var ret = new BlueprintMask(length());
        for (int i = 0, length = length(); i < length; i++) {
            var j = index[i];
            if (j >= 0) ret.set(j);
        }
        return ret;
    }

    /**
     * Get a blueprint mask on those electrode belongs to {@code category}.
     *
     * @param category electrode category
     * @return a blueprint mask.
     * @see #mask(int[], int)
     */
    public final BlueprintMask mask(int category) {
        return mask(blueprint.blueprint, category);
    }

    /**
     * Get a blueprint mask on those electrode belongs to {@code category}.
     *
     * @param blueprint a blueprint int array
     * @param category  electrode category
     * @return a blueprint mask.
     */
    public final BlueprintMask mask(int[] blueprint, int category) {
        return BlueprintMask.eq(blueprint, category);
    }

    /**
     * An electrode stream filtered with a given condiction.
     *
     * @param filter electrode picker
     * @return An electrode stream.
     */
    public final Stream<Electrode> filter(Predicate<Electrode> filter) {
        return blueprint.stream().filter(filter);
    }

    /**
     * get invalid electrode index array for given electrode.
     *
     * @param electrode electrode index.
     * @return electrode index array.
     * @throws RuntimeException missing channelmap
     */
    public int[] invalid(int electrode) {
        var chmap = Objects.requireNonNull(blueprint.chmap, "missing channelmap");
        var electrodes = electrodes();
        var invalid = blueprint.probe.getInvalidElectrodes(chmap, electrodes.get(electrode), electrodes);
        return index(invalid);
    }

    /**
     * get invalid electrode index array for given electrode.
     *
     * @param electrodes restrict subset of electrodes
     * @param index      test electrode index of {@code electrodes}
     * @return electrode index array (domain use all electrode set instead of {@code electrodes}).
     * @throws RuntimeException missing channelmap
     */
    public int[] invalid(List<ElectrodeDescription> electrodes, int index) {
        var chmap = Objects.requireNonNull(blueprint.chmap, "missing channelmap");
        var invalid = blueprint.probe.getInvalidElectrodes(chmap, electrodes.get(index), electrodes);
        return index(invalid);
    }

    /**
     * get invalid electrode index array for given electrode.
     *
     * @param electrodes restrict subset of electrodes
     * @param electrode  test electrode
     * @return electrode index array (domain use all electrode set instead of {@code electrodes}).
     * @throws RuntimeException missing channelmap
     */
    public int[] invalid(List<ElectrodeDescription> electrodes, ElectrodeDescription electrode) {
        var chmap = Objects.requireNonNull(blueprint.chmap, "missing channelmap");
        var invalid = blueprint.probe.getInvalidElectrodes(chmap, electrode, electrodes);
        return index(invalid);
    }

    public int[] invalid(int[] index) {
        var chmap = Objects.requireNonNull(blueprint.chmap, "missing channelmap");
        var electrodes = electrodes();
        var invalid = blueprint.probe.getInvalidElectrodes(chmap, pick(electrodes, index), electrodes);
        return index(invalid);
    }

    public BlueprintMask invalid(BlueprintMask selected) {
        return invalid(selected, true);
    }

    /**
     * @param selected
     * @param include  include selected into invalid results
     * @return
     */
    public BlueprintMask invalid(BlueprintMask selected, boolean include) {
        var chmap = Objects.requireNonNull(blueprint.chmap, "missing channelmap");
        var electrodes = electrodes();
        var invalid = blueprint.probe.getInvalidElectrodes(chmap, pick(electrodes, selected), electrodes);
        var ret = mask(invalid);
        if (!include) {
            ret = ret.diff(selected);
        }
        return ret;
    }

    /**
     * @param category electrode category
     * @return
     */
    public int count(int category) {
        return count(blueprint.blueprint, category);
    }

    public int count(int[] blueprint, int category) {
        if (length() != blueprint.length) throw new RuntimeException();

        var ret = 0;
        for (int c : blueprint) {
            if (c == category) ret++;
        }
        return ret;
    }


    /**
     * @param category
     * @param index    electrode index array
     * @return
     */
    public int count(int category, int[] index) {
        return count(blueprint.blueprint, category, index);
    }

    public int count(int[] blueprint, int category, int[] index) {
        if (length() != blueprint.length) throw new RuntimeException();

        var ret = 0;
        for (int i : index) {
            if (blueprint[i] == category) ret++;
        }
        return ret;
    }

    /**
     * @param category electrode category
     * @param mask
     * @return
     */
    public int count(int category, BlueprintMask mask) {
        return count(blueprint.blueprint, category, mask);
    }

    public int count(int[] blueprint, int category, BlueprintMask mask) {
        if (blueprint.length != mask.length()) throw new RuntimeException();
        return mask(blueprint, category).and(mask).count();
    }

    public final int count(Predicate<Electrode> picker) {
        return (int) filter(picker).count();
    }


    /**
     * {@code a[i] = v}
     *
     * @param a int array
     * @param i {@code a} indexed array
     * @param v value
     * @return {@code a} itself.
     */
    public static int[] set(int[] a, int[] i, int v) {
        for (int j : i) {
            a[j] = v;
        }
        return a;
    }

    public static int[] set(int[] a, int[] i, int[] v) {
        if (i.length != v.length) throw new IllegalArgumentException();
        for (int j = 0, length = i.length; j < length; j++) {
            a[i[j]] = v[j];

        }
        return a;
    }

    /**
     * {@code a[i[offset:offset+length]] = v}
     *
     * @param a      int array
     * @param i      {@code a} indexed array
     * @param offset
     * @param length
     * @param v      value
     * @return {@code a} itself.
     */
    public static int[] set(int[] a, int[] i, int offset, int length, int v) {
        for (int j = 0; j < length; j++) {
            a[i[j + offset]] = v;
        }
        return a;
    }

    public static int[] setIfUnset(int[] a, int[] i, int offset, int length, int v) {
        for (int j = 0; j < length; j++) {
            var k = i[j + offset];
            if (a[k] == 0) a[k] = v;
        }
        return a;
    }


    /*============================*
     * category zone manipulation *
     *============================*/

    public record Movement(int x, int y) {
        public Movement(int y) {
            this(0, y);
        }

        public Movement() {
            this(0, 0);
        }

        public boolean isZero() {
            return x == 0 && y == 0;
        }
    }

    /**
     * @param step y movement
     */
    public final void move(int step) {
        move(new Movement(step));
    }

    public final void move(Movement step) {
        if (step.isZero() || length() == 0) return;
        from(move(ref(), step));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step) {
        return move(blueprint, new Movement(step));
    }

    public final int[] move(int[] blueprint, Movement step) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (step.isZero()) return blueprint.clone();

        var ret = new int[length];
        if (length() == 0) return ret;

        return move(ret, blueprint, step, (_, _) -> 1);
    }


    /**
     * @param step     y movement
     * @param category restrict on category
     */
    public final void move(int step, int category) {
        move(new Movement(step), category);
    }

    public final void move(Movement step, int category) {
        if (step.isZero() || length() == 0) return;
        from(move(ref(), step, category));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param category  restrict on category
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step, int category) {
        return move(blueprint, new Movement(step), category);
    }

    public final int[] move(int[] blueprint, Movement step, int category) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (step.isZero()) return blueprint.clone();

        var ret = new int[length];
        if (length() == 0) return ret;

        return move(ret, blueprint, step, (_, cate) -> cate == category ? 1 : 0);
    }

    /**
     * @param step  y movement
     * @param index electrode indexes
     */
    public final void move(int step, int[] index) {
        move(new Movement(step), index);
    }

    public final void move(Movement step, int[] index) {
        if (step.isZero() || length() == 0 || index.length == 0) return;
        from(move(ref(), step, index));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param index     electrode indexes
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step, int[] index) {
        return move(blueprint, new Movement(step), index);
    }

    public final int[] move(int[] blueprint, Movement step, int[] index) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (step.isZero() || index.length == 0) return blueprint.clone();

        var ret = new int[length];
        if (length == 0) return ret;

        Arrays.sort(index);
        var pointer = new AtomicInteger();

        return move(ret, blueprint, step, (i, _) -> {
            var p = pointer.get();
            if (p < index.length && index[p] == i) {
                return pointer.incrementAndGet();
            } else {
                return 0;
            }
        });
    }

    /**
     * @param step y movement
     * @param mask electrode mask array
     */
    public final void move(int step, BlueprintMask mask) {
        move(new Movement(step), mask);
    }

    public final void move(Movement step, BlueprintMask mask) {
        if (length() != mask.length()) throw new IllegalArgumentException();
        from(move(ref(), step, mask));
    }

    /**
     * @param blueprint
     * @param step      y movement
     * @param mask      electrode mask array
     * @return moved result, a copied {@code blueprint}.
     */
    public final int[] move(int[] blueprint, int step, BlueprintMask mask) {
        return move(blueprint, new Movement(step), mask);
    }

    public final int[] move(int[] blueprint, Movement step, BlueprintMask mask) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (length != mask.length()) throw new RuntimeException();
        if (step.isZero()) return blueprint.clone();

        var ret = new int[length];
        if (length() == 0) return ret;

        var x = ProbeDescription.CATE_UNSET;
        return move(ret, blueprint, step, (i, cate) -> (cate != x && mask.test(i)) ? 1 : 0);
    }

    /**
     * move electrodes along y-axis.
     * <br/>
     * This method does not reset the content of {@code output}.
     *
     * @param output    output blueprint.
     * @param blueprint source blueprint.
     * @param step      y movement
     * @param tester    whether move this electrode with the signature {@code (electrode, category) -> 1_or_0}.
     * @return {@code output} itself.
     */
    protected int[] move(int[] output, int[] blueprint, Movement step, IntBinaryOperator tester) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();
        if (length != output.length) throw new RuntimeException();
        if (length == 0) return output;
        if (step.isZero()) {
            if (output != blueprint) {
                System.arraycopy(blueprint, 0, output, 0, length);
            }
            return output;
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dx = dx();
        var dy = dy();

        for (int i = 0; i < length; i++) {
            int j = i;
            var cate = blueprint[i];

            if (tester.applyAsInt(i, cate) > 0) {
                var s = shank[i];
                var x = (int) (posx[i] + step.x * dx);
                var y = (int) (posy[i] + step.y * dy);
                j = index(s, x, y);
            }

            if (j >= 0 && output[j] == ProbeDescription.CATE_UNSET) {
                output[j] = cate;
            }
        }

        return output;
    }

    /**
     * move electrodes index along y-axis.
     *
     * @param index electrode index.
     * @param step  y movement
     * @return moved electrode index. {@code -1} when it outside the probe.
     */
    public int[] moveIndex(int[] index, Movement step) {
        if (index.length == 0) return index;
        if (step.isZero()) {
            return index;
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dx = dx();
        var dy = dy();
        var ret = new int[index.length];

        for (int i = 0, length = index.length; i < length; i++) {
            int j = index[i];

            var s = shank[j];
            var x = (int) (posx[i] + step.x * dx);
            var y = (int) (posy[i] + step.y * dy);
            ret[i] = index(s, x, y);
        }

        return ret;
    }

    /**
     * move electrodes index along y-axis.
     *
     * @param output output electrode index.
     * @param index  source electrode index.
     * @param step   movement
     * @return number of index filled in {@code output}.
     */
    public int moveIndex(int[] output, int[] index, Movement step) {
        if (index.length == 0) return 0;
        if (step.isZero()) {
            if (output != index) {
                System.arraycopy(index, 0, output, 0, index.length);
            }
            return index.length;
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dx = dx();
        var dy = dy();
        var ret = 0;

        for (var i : index) {
            var s = shank[i];
            var x = (int) (posx[i] + step.x * dx);
            var y = (int) (posy[i] + step.y * dy);
            var j = index(s, x, y);

            if (j >= 0) {
                output[ret++] = j;
            }
        }

        return ret;
    }

    public BlueprintMask moveMask(BlueprintMask mask, Movement step) {
        if (length() != mask.length()) throw new IllegalArgumentException();
        if (step.isZero()) {
            return mask;
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dx = dx();
        var dy = dy();
        var ret = new BlueprintMask(length());

        mask.forEach(i -> {
            var s = shank[i];
            var x = (int) (posx[i] + step.x * dx);
            var y = (int) (posy[i] + step.y * dy);
            var j = index(s, x, y);

            if (j >= 0) {
                ret.set(j);
            }
        });

        return ret;
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
                output[2 * i] = surrounding(s, x, y, i * 2);
                // 1, 3, 5, 7
                output[2 * i + 1] = -1;
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
        var dx = (int) dx();
        var dy = (int) dy();
        var d0 = 0;
        return switch (c % 8) {
            case 0 -> index(s, x + dx, y + d0);
            case 1, -7 -> index(s, x + dx, y + dy);
            case 2, -6 -> index(s, x + d0, y + dy);
            case 3, -5 -> index(s, x - dx, y + dy);
            case 4, -4 -> index(s, x - dx, y + d0);
            case 5, -3 -> index(s, x - dx, y - dy);
            case 6, -2 -> index(s, x + d0, y - dy);
            case 7, -1 -> index(s, x + dx, y - dy);
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
    public final Clustering findClustering(boolean diagonal) {
        return findClustering(ref(), diagonal);
    }

    public Clustering findClustering(int[] blueprint, boolean diagonal) {
        var length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        var x = ProbeDescription.CATE_UNSET;
        return findClustering(blueprint, diagonal, (_, cate) -> cate != x ? 1 : 0);
    }

    public final Clustering findClustering(int category, boolean diagonal) {
        return findClustering(ref(), category, diagonal);
    }

    public Clustering findClustering(int[] blueprint, int category, boolean diagonal) {
        int length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        return findClustering(blueprint, diagonal, (_, cate) -> cate == category ? 1 : 0);
    }

    protected Clustering findClustering(int[] blueprint, boolean diagonal, IntBinaryOperator tester) {
        int length = blueprint.length;
        if (length == 0) return Clustering.EMPTY;

        var n = (int) IntStream.range(0, length)
            .filter(i -> tester.applyAsInt(i, blueprint[i]) > 0)
            .count();

        var ret = new Clustering(length);
        if (n == 0) {
            return ret;
        } else if (n == length) {  // electrodes are belonging to same the category.
            ret.fill(1);
            return ret;
        }

        var clustering = ret.clustering();
        for (int i = 0; i < length; i++) {
            if (tester.applyAsInt(i, blueprint[i]) > 0) {
                clustering[i] = 1;
            }
        }

        var surr = new int[8];
        var group = 1;
        for (int i = 0; i < length; i++) {
            if (clustering[i] > 0) {
                int cate = blueprint[i];
                surrounding(i, diagonal, surr);
                var g = ++group;
                for (var j : surr) {
                    if (j >= 0 && clustering[j] > 1 && blueprint[j] == cate) {
                        g = Math.min(g, clustering[j]);
                    }
                }
                if (clustering[i] > g) {
                    ret.unionClusteringGroup(clustering[i], g);
                } else {
                    clustering[i] = g;
                }

                for (var j : surr) {
                    if (j >= 0 && clustering[j] > 0 && blueprint[j] == cate) {
                        if (clustering[j] > g) {
                            ret.unionClusteringGroup(clustering[j], g);
                        } else {
                            clustering[j] = g;
                        }
                    }
                }
            }
        }

        return ret;
    }


    public final List<ClusteringEdges> getClusteringEdges() {
        if (length() == 0) return List.of();
        return getClusteringEdges(findClustering(false));
    }

    public final List<ClusteringEdges> getClusteringEdges(int[] blueprint) {
        if (length() != blueprint.length) throw new RuntimeException();
        if (blueprint.length == 0) return List.of();
        var clustering = findClustering(blueprint, false);
        return getClusteringEdges(clustering);
    }

    public final List<ClusteringEdges> getClusteringEdges(int category) {
        if (length() == 0) return List.of();
        return getClusteringEdges(findClustering(category, false));
    }

    public final List<ClusteringEdges> getClusteringEdges(int[] blueprint, int category) {
        if (length() != blueprint.length) throw new RuntimeException();
        if (blueprint.length == 0) return List.of();
        return getClusteringEdges(findClustering(blueprint, category, false));
    }

    public List<ClusteringEdges> getClusteringEdges(Clustering clustering) {
        var mode = clustering.modeGroup();
        if (mode.group() == 0) return List.of();

        var src = ref();
        var shank = shank();
        var posx = posx();
        var posy = posy();

        var mask = new BlueprintMask(length());
        var ret = new ArrayList<ClusteringEdges>();

        for (var g : clustering.groups()) {
            clustering.maskGroup(mask, g);
            var count = mask.count();
            if (count == 0) continue;

            var i = mask.nextSetIndex(0);
            int s = shank[i];
            int c = src[i];

            if (count == 1) {
                var x = posx[i];
                var y = posy[i];
                ret.add(ClusteringUtils.pointClustering(c, s, x, y));
            } else {
                ret.add(ClusteringUtils.areaClustering(this, c, s, mask));
            }
        }

        return ret;
    }

    /*=================*
     * fill clustering *
     *=================*/

    public final void fillClusteringEdges(List<ClusteringEdges> edges) {
        var blueprint = ref();
        for (var edge : edges) {
            fillClusteringEdges(blueprint, edge);
        }
    }

    /**
     * @param blueprint output blueprint.
     * @param edges
     * @return
     */
    public final int[] fillClusteringEdges(int[] blueprint, List<ClusteringEdges> edges) {
        for (var edge : edges) {
            fillClusteringEdges(blueprint, edge);
        }
        return blueprint;
    }

    public final void fillClusteringEdges(List<ClusteringEdges> edges, int category) {
        var clustering = edges.stream().map(it -> it.withCategory(category)).toList();
        fillClusteringEdges(clustering);
    }

    /**
     * @param blueprint output blueprint.
     * @param edges
     * @param category  overwrite category.
     * @return
     */
    public final int[] fillClusteringEdges(int[] blueprint, List<ClusteringEdges> edges, int category) {
        var clustering = edges.stream().map(it -> it.withCategory(category)).toList();
        return fillClusteringEdges(blueprint, clustering);
    }

    public final void fillClusteringEdges(ClusteringEdges edge) {
        fillClusteringEdges(ref(), edge);
    }

    /**
     * @param blueprint output blueprint.
     * @param edge
     * @return {@code blueprint} itself.
     */
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

    public record AreaThreshold(int lower, int upper) implements DoublePredicate {
        public static final AreaThreshold ALL = new AreaThreshold(0, Integer.MAX_VALUE);

        public AreaThreshold(int upper) {
            this(0, upper);
        }

        @Override
        public boolean test(double value) {
            return lower <= value && value <= upper;
        }
    }

    /**
     * fill all category zones as rectangle.
     */
    public final void fill() {
        from(fill(ref(), AreaThreshold.ALL));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param category
     */
    public final void fill(int category) {
        from(fill(ref(), category, AreaThreshold.ALL));
    }

    /**
     * fill all category zones as rectangle.
     *
     * @param blueprint
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint) {
        return fill(blueprint, AreaThreshold.ALL);
    }

    /**
     * fill category zone as rectangle.
     *
     * @param blueprint
     * @param category
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint, int category) {
        return fill(blueprint, category, AreaThreshold.ALL);
    }

    /**
     * fill all category zones as rectangle.
     *
     * @param threshold
     */
    public final void fill(AreaThreshold threshold) {
        from(fill(ref(), threshold));
    }

    /**
     * fill category zone as rectangle.
     *
     * @param category
     * @param threshold
     */
    public final void fill(int category, AreaThreshold threshold) {
        from(fill(ref(), category, threshold));
    }

    /**
     * fill all category zones as rectangle.
     *
     * @param blueprint
     * @param threshold
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, true);
        return fill(blueprint, clustering, threshold);
    }

    /**
     * fill category zone as rectangle.
     *
     * @param blueprint
     * @param category
     * @param threshold
     * @return filled result, a copied {@code blueprint}.
     */
    public final int[] fill(int[] blueprint, int category, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, category, true);
        return fill(blueprint, clustering, threshold);
    }

    public int[] fill(int[] blueprint, Clustering clustering, AreaThreshold threshold) {
        var shank = shank();
        var posx = posx();
        var posy = posy();

        for (var group : clustering.groups()) {
            if (threshold.test(clustering.groupCount(group))) {
                var index = clustering.indexGroup(group);
                var s = shank[index[0]];
                var c = blueprint[index[0]];
                var x = Arrays.stream(index).map(i -> posx[i]).boxed().gather(MinMaxInt.intMinmax()).findFirst().get();
                var y = Arrays.stream(index).map(i -> posy[i]).boxed().gather(MinMaxInt.intMinmax()).findFirst().get();

                fillClusteringEdges(blueprint, new ClusteringEdges(c, s, List.of(
                    new ClusteringEdges.Corner(x.max(), y.max(), 1),
                    new ClusteringEdges.Corner(x.min(), y.max(), 3),
                    new ClusteringEdges.Corner(x.min(), y.min(), 5),
                    new ClusteringEdges.Corner(x.max(), y.min(), 7)
                )));
            }
        }
        return blueprint;
    }

    public record AreaChange(int up, int down, int left, int right) implements Iterable<Movement> {
        public AreaChange {
            if (up < 0) throw new IllegalArgumentException();
            if (down < 0) throw new IllegalArgumentException();
            if (left < 0) throw new IllegalArgumentException();
            if (right < 0) throw new IllegalArgumentException();
        }

        public AreaChange(int x, int y) {
            this(y, y, x, x);
        }

        public AreaChange(int y) {
            this(y, y, 0, 0);
        }

        public AreaChange() {
            this(0, 0, 0, 0);
        }

        public boolean isZero() {
            return up == 0 && down == 0 && left == 0 && right == 0;
        }

        public boolean isXZero() {
            return left == 0 && right == 0;
        }

        public boolean isYZero() {
            return up == 0 && down == 0;
        }

        public AreaChange invert() {
            return new AreaChange(down, up, right, left);
        }

        @Override
        public Iterator<Movement> iterator() {
            return new Iterator<>() {
                private int x = -left;
                private int y = -down;

                @Override
                public boolean hasNext() {
                    return x <= right && y <= up;
                }

                @Override
                public Movement next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    var ret = new Movement(x, y);
                    if (++x > right) {
                        x = -left;
                        y++;
                    }
                    return ret;
                }
            };
        }
    }

    public final void extend(int category, int step) {
        from(extend(ref(), category, new AreaChange(step), category, AreaThreshold.ALL));
    }

    public final void extend(int category, int step, int value) {
        from(extend(ref(), category, new AreaChange(step), value, AreaThreshold.ALL));
    }

    /**
     * @param blueprint
     * @param category
     * @param step
     * @return extended result, a copied {@code blueprint}.
     */
    public final int[] extend(int[] blueprint, int category, int step) {
        return extend(blueprint, category, new AreaChange(step), category, AreaThreshold.ALL);
    }

    public final int[] extend(int[] blueprint, int category, int step, int value) {
        return extend(blueprint, category, new AreaChange(step), value, AreaThreshold.ALL);
    }

    /**
     * @param category
     * @param step
     * @param threshold
     */
    public final void extend(int category, AreaChange step, AreaThreshold threshold) {
        extend(category, step, category, threshold);
    }

    public final void extend(int category, AreaChange step, int value, AreaThreshold threshold) {
        from(extend(ref(), category, step, value, threshold));
    }

    /**
     * @param blueprint
     * @param category
     * @param step
     * @param threshold
     * @return extended result, a copied {@code blueprint}.
     */
    public final int[] extend(int[] blueprint, int category, AreaChange step, int value, AreaThreshold threshold) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step.isZero() || length == 0) return ret;

        return extend(ret, blueprint, category, step, value, threshold);
    }

    /**
     * extend category zones.
     * <br/>
     * This method does not reset the content of {@code output}.
     *
     * @param output    output blueprint
     * @param blueprint source blueprint
     * @param category
     * @param step
     * @param threshold
     * @return {@code output} itself.
     */
    protected int[] extend(int[] output, int[] blueprint, int category, AreaChange step, int value, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var index = clustering.indexGroup();
        var move = new int[index.length];
        for (var m : step) {
            var size = moveIndex(move, index, m);
            setIfUnset(output, move, 0, size, value);
        }
        return output;
    }

    public final void reduce(int category, int step) {
        from(reduce(ref(), category, new AreaChange(step), ProbeDescription.CATE_UNSET, AreaThreshold.ALL));
    }

    public final void reduce(int category, int step, int value) {
        from(reduce(ref(), category, new AreaChange(step), value, AreaThreshold.ALL));
    }

    public final int[] reduce(int[] blueprint, int category, int step) {
        return reduce(blueprint, category, new AreaChange(step), ProbeDescription.CATE_UNSET, AreaThreshold.ALL);
    }

    public final int[] reduce(int[] blueprint, int category, int value, int step) {
        return reduce(blueprint, category, new AreaChange(step), value, AreaThreshold.ALL);
    }


    public final void reduce(int category, AreaChange step, AreaThreshold threshold) {
        from(reduce(ref(), category, step, ProbeDescription.CATE_UNSET, threshold));
    }

    public final void reduce(int category, AreaChange step, int value, AreaThreshold threshold) {
        from(reduce(ref(), category, step, value, threshold));
    }


    public final int[] reduce(int[] blueprint, int category, AreaChange step, AreaThreshold threshold) {
        return reduce(blueprint, category, step, ProbeDescription.CATE_UNSET, threshold);
    }

    public final int[] reduce(int[] blueprint, int category, AreaChange step, int value, AreaThreshold threshold) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (step.isZero() || length == 0) return ret;

        return reduce(ret, blueprint, category, step, value, threshold);
    }


    /**
     * reduce category zones.
     * <br/>
     * This method does not reset the content of {@code output}.
     *
     * @param output    output blueprint
     * @param blueprint source blueprint
     * @param category
     * @param step
     * @param threshold
     * @return {@code output} itself.
     */
    protected int[] reduce(int[] output, int[] blueprint, int category, AreaChange step, int value, AreaThreshold threshold) {
        if (step.isZero() || length() == 0) return output;

        var clustering = findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var from = clustering.maskGroup();
        var mark = new BlueprintMask(from);
        if (step.left > 0) {
            var move = moveMask(from, new Movement(step.left, 0)); // move right
            mark.iand(move);
        }

        if (step.right > 0) {
            var move = moveMask(from, new Movement(-step.right, 0)); // move left
            mark.iand(move);
        }

        if (step.up > 0) {
            var move = moveMask(from, new Movement(0, -step.up)); // move down
            mark.iand(move);
        }

        if (step.down > 0) {
            var move = moveMask(from, new Movement(0, step.down)); // move up
            mark.iand(move);
        }

        from.idiff(mark).fill(output, value);

        return output;
    }

    public final void reduce(int category, AreaThreshold threshold) {
        from(reduce(ref(), category, threshold));
    }

    public final int[] reduce(int[] blueprint, int category, AreaThreshold threshold) {
        int length = length();
        if (length != blueprint.length) throw new RuntimeException();

        var ret = blueprint.clone();
        if (length == 0) return ret;

        return reduce(ret, blueprint, category, threshold);
    }

    protected int[] reduce(int[] output, int[] blueprint, int category, AreaThreshold threshold) {
        var clustering = findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var index = clustering.indexGroup();
        set(output, index, ProbeDescription.CATE_UNSET);

        return output;
    }

    /*=========*
     * File IO *
     *=========*/

    public int[] loadBlueprint(Path file) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);
        if (suffix.equals(".npy")) {
            try {
                return loadNumpyBlueprint(file);
            } catch (UnsupportedNumpyDataFormatException e) {
            }
        } else if (suffix.equals(".csv") || suffix.equals(".tsv")) {
            return loadCsvBlueprint(file);
        }

        var bp = new Blueprint<>(blueprint);
        bp.load(file);
        return bp.blueprint;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     * @throws UnsupportedNumpyDataFormatException
     */
    public double[] loadBlueprintData(Path file) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);
        if (suffix.equals(".npy")) {
            return loadNumpyBlueprintData(file);
        } else if (suffix.equals(".csv") || suffix.equals(".tsv")) {
            return loadCsvBlueprintData(file);
        } else {
            throw new RuntimeException("unknown data format " + suffix);
        }
    }

    public int[] loadNumpyBlueprint(Path file) throws IOException {
        var result = Numpy.read(file, header -> switch (header.ndim()) {
            case 1 -> Numpy.ofInt();
            case 2 -> Numpy.ofD2Int(true);
            default -> throw new UnsupportedNumpyDataFormatException(header, "unknown shape");
        });

        return switch (result.ndim()) {
            case 1 -> (int[]) result.data();
            case 2 -> ((int[][]) result.data())[4];
            default -> throw new RuntimeException("unknown result");
        };
    }

    public void saveNumpyBlueprint(Path file, int[] blueprint) throws IOException {
        Numpy.write(file, blueprint);
    }

    public double[] loadNumpyBlueprintData(Path file) throws IOException {
        var result = Numpy.read(file, header -> switch (header.ndim()) {
            case 1 -> Numpy.ofDouble();
            case 2 -> Numpy.ofD2Double(true);
            default -> throw new UnsupportedNumpyDataFormatException(header, "unknown shape");
        });

        return switch (result.ndim()) {
            case 1 -> (double[]) result.data();
            case 2 -> ((double[][]) result.data())[4];
            default -> throw new RuntimeException("unknown result");
        };
    }

    public int[] loadCsvBlueprint(Path file) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);

        boolean tsv;
        if (suffix.equals(".csv")) {
            tsv = false;
        } else if (suffix.equals(".tsv")) {
            tsv = true;
        } else {
            throw new RuntimeException("unknown file format : " + filename);
        }

        var ret = empty();
        loadCsvBlueprint(file, tsv, (j, s) -> {
            ret[j] = Integer.parseInt(s);
        });
        return ret;
    }

    public double[] loadCsvBlueprintData(Path file) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);

        boolean tsv;
        if (suffix.equals(".csv")) {
            tsv = false;
        } else if (suffix.equals(".tsv")) {
            tsv = true;
        } else {
            throw new RuntimeException("unknown file format : " + filename);
        }

        var ret = new double[length()];
        loadCsvBlueprint(file, tsv, (j, s) -> {
            ret[j] = Double.parseDouble(s);
        });
        return ret;
    }

    private void loadCsvBlueprint(Path file, boolean tsv, BiConsumer<Integer, String> consumer) throws IOException {
        var format = getCsvFormat(tsv).get();

        try (var reader = Files.newBufferedReader(file)) {
            var parse = format.parse(reader);
            var header = parse.getHeaderNames();
            if (!checkCsvHeader(header)) throw new IOException("unknown header : " + header);

            for (var record : parse) {
                int s, x, y;
                try {
                    s = Integer.parseInt(record.get(header.get(0)));
                    x = Integer.parseInt(record.get(header.get(1)));
                    y = Integer.parseInt(record.get(header.get(2)));

                    var i = index(s, x, y);
                    if (i >= 0) {
                        consumer.accept(i, record.get(header.get(3)));
                    }
                } catch (NumberFormatException e) {
                    var line = parse.getCurrentLineNumber();
                    throw new IOException("bad numbers at line " + line, e);
                }


            }
        }
    }

    private static CSVFormat.Builder getCsvFormat(boolean tsv) {
        return CSVFormat.DEFAULT.builder()
            .setCommentMarker('#')
            .setIgnoreSurroundingSpaces(true)
            .setIgnoreHeaderCase(true)
            .setDelimiter(tsv ? '\t' : ',');
    }

    private static boolean checkCsvHeader(List<String> header) {
        if (header.size() != 4) return false;
        var field = header.get(0);
        if (!(field.equals("s") || field.equals("shank"))) {
            return false;
        }
        field = header.get(1);
        if (!field.equals("x")) return false;
        field = header.get(2);
        if (!field.equals("y")) return false;
        return true;
    }

    public void saveCsvBlueprint(Path file, int[] blueprint) throws IOException {
        var filename = file.getFileName().toString();
        var i = filename.lastIndexOf('.');
        var suffix = filename.substring(i);
        if (suffix.equals(".csv")) {
            saveCsvBlueprint(file, blueprint, false);
        } else if (suffix.equals(".tsv")) {
            saveCsvBlueprint(file, blueprint, true);
        } else {
            throw new RuntimeException("unknown file format : " + filename);
        }
    }

    private void saveCsvBlueprint(Path file, int[] blueprint, boolean tsv) throws IOException {
        if (length() != blueprint.length) throw new IllegalArgumentException();

        var format = getCsvFormat(tsv)
            .setHeader("s", "x", "y", "c")
            .get();

        try (var writer = Files.newBufferedWriter(file, CREATE, TRUNCATE_EXISTING, WRITE);
             var printer = new CSVPrinter(writer, format)) {
            var shank = shank();
            var posx = posx();
            var posy = posy();

            for (int i = 0, length = blueprint.length; i < length; i++) {
                printer.printRecord(shank[i], posx[i], posy[i], blueprint[i]);
            }
        }
    }

    /*==================================*
     * blueprint-like data manipulation *
     *==================================*/

    public record ElectrodeData(Electrode e, double v) {
        public int i() {
            return e.i();
        }

        public int s() {
            return e.s();
        }

        public int x() {
            return e.x();
        }

        public int y() {
            return e.y();
        }

        public int c() {
            return e.c();
        }
    }

    public Stream<ElectrodeData> stream(double[] data) {
        if (length() != data.length) throw new RuntimeException();
        return stream().map(e -> new ElectrodeData(e, data[e.i()]));
    }

    /**
     * extra value from {@code data} with given index.
     * <br/>
     * If index out of array boundary, {@link Double#NaN} is used.
     *
     * @param data
     * @param index
     * @return
     */
    public double[] get(double[] data, int[] index) {
        var ret = new double[index.length];
        for (int i = 0, length = index.length; i < length; i++) {
            var j = index[i];
            if (j >= 0 && j < length) {
                ret[i] = data[j];
            } else {
                ret[i] = Double.NaN;
            }
        }
        return ret;
    }

    /**
     * extra value from {@code data} with given channelmap electrodes.
     * <br/>
     * If index out of array boundary, {@link Double#NaN} is used.
     *
     * @param data
     * @param chmap
     * @return
     */
    public double[] get(double[] data, T chmap) {
        return get(data, blueprint.probe.allChannels(chmap));
    }

    /**
     * extra value from {@code data} with given electrodes.
     * <br/>
     * If index out of array boundary, {@link Double#NaN} is used.
     *
     * @param data
     * @param e
     * @return
     */
    public double[] get(double[] data, List<ElectrodeDescription> e) {
        return e.stream()
            .mapToInt(it -> index(it.s(), it.x(), it.y()))
            .mapToDouble(i -> i < 0 ? Double.NaN : data[i])
            .toArray();
    }

    public double[] get(double[] data, BlueprintMask mask) {
        return mask.squeeze(data);
    }

    public double[] get(double[] data, Predicate<Electrode> pick) {
        return stream().filter(pick)
            .mapToDouble(e -> data[e.i()])
            .toArray();
    }

    /**
     * {@code a[i] = v}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param data double array
     * @param i    {@code a} indexed array
     * @param v    value
     * @return {@code a} itself.
     */
    public double[] set(double[] data, int[] i, double v) {
        for (int j = 0, length = i.length; j < length; j++) {
            var k = i[j];
            if (k >= 0 && k < length) {
                data[k] = v;
            }
        }
        return data;
    }


    public double[] set(double[] data, T chmap, double v) {
        return set(data, blueprint.probe.allChannels(chmap), v);
    }

    public double[] set(double[] data, List<ElectrodeDescription> e, double v) {
        for (var k : e) {
            var i = index(k.s(), k.x(), k.y());
            if (i >= 0 && i < data.length) {
                data[i] = v;
            }
        }
        return data;
    }


    /**
     * {@code a[i] = v[:]}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param data
     * @param i
     * @param v
     * @return {@code a} itself
     * @throws IllegalArgumentException the length of {@code i} and {@code v} does not agree.
     */
    public double[] set(double[] data, int[] i, double[] v) {
        if (i.length != v.length) throw new IllegalArgumentException();
        for (int j = 0, length = i.length; j < length; j++) {
            var k = i[j];
            if (k >= 0 && k < length) {
                data[k] = v[j];
            }
        }
        return data;
    }

    /**
     * {@code a[i[offset:offset+length]] = v}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param data   double array
     * @param i      {@code a} indexed array
     * @param offset start index of {@code i}
     * @param length
     * @param v      value
     * @return {@code a} itself.
     * @throws IllegalArgumentException       when negative {@code length}
     * @throws ArrayIndexOutOfBoundsException {@code offset} and {@code length} out of {@code i}'s bounds.
     */
    public double[] set(double[] data, int[] i, int offset, int length, double v) {
        if (length < 0) throw new IllegalArgumentException();
        var _ = i[offset];
        var _ = i[offset + length - 1];

        for (int j = 0; j < length; j++) {
            var k = i[j + offset];
            if (k >= 0 && k < length) {
                data[k] = v;
            }
        }
        return data;
    }


    public double[] set(double[] data, BlueprintMask mask, double v) {
        mask.fill(data, v);
        return data;
    }

    public double[] set(double[] data, BlueprintMask mask, double[] v) {
        mask.where(data, v, null);
        return data;
    }

    public double[] set(double[] data, BlueprintMask writeMask, double[] v, BlueprintMask readMask) {
        writeMask.where(data, v, readMask);
        return data;
    }

    /**
     * {@code a[i[offset:offset+length]] = oper(a[i[offset:offset+length]])}
     * <br/>
     * If an index out of array boundary, it is ignored.
     *
     * @param data   double array
     * @param i      {@code a} indexed array
     * @param offset
     * @param length
     * @param oper   value operator
     * @return {@code a} itself.
     */
    public double[] set(double[] data, int[] i, int offset, int length, DoubleUnaryOperator oper) {
        if (length < 0) throw new IllegalArgumentException();
        var _ = i[offset];
        var _ = i[offset + length - 1];

        for (int j = 0; j < length; j++) {
            var k = i[j + offset];
            if (k >= 0 && k < length) {
                data[k] = oper.applyAsDouble(data[k]);
            }
        }
        return data;
    }

    public enum InterpolateMethod {
        zero, mean, median, min, max;

    }

    private static ToDoubleFunction<double[]> getInterpolateMethod(InterpolateMethod f) {
        return switch (f) {
            case zero -> _ -> 0;
            case mean -> kernel -> {
                double ret = 0;
                int cnt = 0;
                for (double v : kernel) {
                    if (!Double.isNaN(v)) {
                        ret += v;
                        cnt++;
                    }
                }
                return cnt == 0 ? Double.NaN : ret / cnt;
            };
            case median -> kernel -> {
                Arrays.sort(kernel); // NaN are put at last.
                var size = kernel.length;
                while (size > 0 && Double.isNaN(kernel[size - 1])) {
                    size--;
                }
                if (size == 0) return Double.NaN;
                if (size == 1) return kernel[0];
                if (size % 2 == 1) {
                    return kernel[size / 2];
                } else {
                    var i = size / 2;
                    return (kernel[i] + kernel[i - 1]) / 2;
                }
            };
            case min -> kernel -> {
                double ret = Double.MAX_VALUE;
                int cnt = 0;
                for (double v : kernel) {
                    if (!Double.isNaN(v)) {
                        ret = Math.min(ret, v);
                        cnt++;
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
            case max -> kernel -> {
                double ret = Double.MIN_VALUE;
                int cnt = 0;
                for (double v : kernel) {
                    if (!Double.isNaN(v)) {
                        ret = Math.max(ret, v);
                        cnt++;
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
        };
    }

    public double[] interpolateNaN(double[] a, int k, InterpolateMethod f) {
        var m = getInterpolateMethod(f);
        return interpolateNaN(a, k, m);
    }

    public final double[] interpolateNaN(double[] a, int k, ToDoubleFunction<double[]> f) {
        if (k % 2 != 1) throw new IllegalArgumentException("k should be a odd number, but " + k);
        return interpolateNaN(a, new double[k], f);
    }

    public final double[] interpolateNaN(double[] a, double[] k, ToDoubleFunction<double[]> f) {
        return interpolateNaN(new double[a.length], a, k, f);
    }

    public double[] interpolateNaN(double[] o, double[] a, double[] k, ToDoubleFunction<double[]> f) {
        if (k.length % 2 != 1) throw new IllegalArgumentException("k should be a odd number, but " + k);

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dy = dy();

        for (int i = 0, length = a.length; i < length; i++) {
            if (Double.isNaN(a[i])) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i];

                for (int j = 0, kn = k.length; j < kn; j++) {
                    var n = j - kn / 2;
                    if (n == 0) {
                        k[j] = Double.NaN;
                    } else {
                        var yy = (int) (y + dy * n);
                        var jj = index(s, x, yy);
                        if (jj >= 0) {
                            k[j] = a[jj];
                        } else {
                            k[j] = Double.NaN;
                        }
                    }
                }

                o[i] = f.applyAsDouble(k);
            } else {
                o[i] = a[i];
            }
        }
        return o;
    }

    private static ToDoubleFunction<double[][]> getInterpolateMethod(int kx, int ky, InterpolateMethod f) {
        return switch (f) {
            case zero -> _ -> 0;
            case mean -> kernel -> {
                double ret = 0;
                int cnt = 0;
                for (double[] vx : kernel) {
                    for (var v : vx) {
                        if (!Double.isNaN(v)) {
                            ret += v;
                            cnt++;
                        }
                    }
                }
                return cnt == 0 ? Double.NaN : ret / cnt;
            };
            case median -> new ToDoubleFunction<>() {
                private final double[] tmp = new double[kx * ky];
                private final ToDoubleFunction<double[]> func = getInterpolateMethod(InterpolateMethod.median);

                @Override
                public double applyAsDouble(double[][] kernel) {
                    var p = 0;
                    for (int i = 0; i < kx; i++) {
                        for (int j = 0; j < ky; j++) {
                            tmp[p++] = kernel[i][j];
                        }
                    }

                    return func.applyAsDouble(tmp);
                }
            };
            case min -> kernel -> {
                double ret = Double.MAX_VALUE;
                int cnt = 0;
                for (double[] vv : kernel) {
                    for (var v : vv) {
                        if (!Double.isNaN(v)) {
                            ret = Math.min(ret, v);
                            cnt++;
                        }
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
            case max -> kernel -> {
                double ret = Double.MIN_VALUE;
                int cnt = 0;
                for (double[] vv : kernel) {
                    for (var v : vv) {
                        if (!Double.isNaN(v)) {
                            ret = Math.max(ret, v);
                            cnt++;
                        }
                    }
                }
                return cnt == 0 ? Double.NaN : ret;
            };
        };
    }

    public double[] interpolateNaN(double[] a, int kx, int ky, InterpolateMethod f) {
        var m = getInterpolateMethod(kx, ky, f);
        return interpolateNaN(a, kx, ky, m);
    }

    public final double[] interpolateNaN(double[] a, int kx, int ky, ToDoubleFunction<double[][]> f) {
        if (kx % 2 != 1) throw new IllegalArgumentException();
        if (ky % 2 != 1) throw new IllegalArgumentException();
        var k = new double[kx][];
        for (int i = 0; i < kx; i++) {
            k[i] = new double[ky];
        }
        return interpolateNaN(a, k, f);
    }

    public final double[] interpolateNaN(double[] a, double[][] k, ToDoubleFunction<double[][]> f) {
        return interpolateNaN(new double[a.length], a, k, f);
    }

    public double[] interpolateNaN(double[] o, double[] a, double[][] k, ToDoubleFunction<double[][]> f) {
        var kx = k.length;
        if (kx % 2 != 1) throw new IllegalArgumentException();
        var ky = k[0].length;
        if (ky % 2 != 1) throw new IllegalArgumentException();
        for (int i = 1; i < kx; i++) {
            if (k[i].length != ky) throw new IllegalArgumentException();
        }

        var shank = shank();
        var posx = posx();
        var posy = posy();
        var dx = dx();
        var dy = dy();

        for (int i = 0, length = a.length; i < length; i++) {
            if (Double.isNaN(a[i])) {
                var s = shank[i];
                var x = posx[i];
                var y = posy[i];

                for (int jx = 0; jx < kx; jx++) {
                    for (int jy = 0; jy < ky; jy++) {
                        var nx = jx - kx / 2;
                        var ny = jy - ky / 2;
                        if (nx == 0 && ny == 0) {
                            k[jx][jy] = Double.NaN;
                        } else {
                            var xx = (int) (x + dx * nx);
                            var yy = (int) (y + dy * ny);
                            var jj = index(s, xx, yy);
                            if (jj >= 0) {
                                k[jx][jy] = a[jj];
                            } else {
                                k[jx][jy] = Double.NaN;
                            }
                        }
                    }
                }

                o[i] = f.applyAsDouble(k);
            } else {
                o[i] = a[i];
            }
        }
        return a;
    }

    /*================*
     * fake blueprint *
     *================*/

    public static BlueprintToolkit<Object> dummy(int ns, int ny, int nx) {
        return new BlueprintToolkit<>(new Blueprint<>(new DummyProbe(ns, nx, ny), new Object()));
    }

    public static BlueprintToolkit<Object> dummy(FlatIntArray image) {
        if (image.ndim() != 2) throw new IllegalArgumentException("not a 2d image");
        var shape = image.shape();
        var ret = new BlueprintToolkit<>(new Blueprint<>(new DummyProbe(1, shape[1], shape[0]), new Object()));
        ret.from(image.array());
        return ret;
    }
}
