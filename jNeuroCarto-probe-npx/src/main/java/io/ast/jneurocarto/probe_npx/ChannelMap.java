package io.ast.jneurocarto.probe_npx;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.probe_npx.io.Imro;
import io.ast.jneurocarto.probe_npx.io.Meta;

@NullMarked
public class ChannelMap implements Iterable<@Nullable Electrode> {

    private final NpxProbeType type;
    private final @Nullable Electrode[] electrodes;
    private int reference = 0;
    private @Nullable NpxMeta meta = null;

    public ChannelMap(NpxProbeType type) {
        this.type = type;
        electrodes = new Electrode[type.nChannel()];
    }

    public ChannelMap(ChannelMap map) {
        this(map.type, map.channels(), map.meta);
        reference = map.reference;
    }

    public ChannelMap(NpxProbeType type, List<@Nullable Electrode> electrodes, @Nullable NpxMeta meta) {
        this(type);

        this.meta = meta;

        for (var electrode : electrodes) {
            if (electrode != null) {
                addElectrode(electrode);
            }
        }
    }

    public static ChannelMap fromImro(String source) {
        try {
            return Imro.read(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ChannelMap fromFile(Path file) throws IOException {
        var filename = file.getFileName().toString();
        if (filename.endsWith(".imro")) {
            return Imro.read(file);
        } else if (filename.endsWith(".meta")) {
            return Meta.read(file);
        } else {
            throw new RuntimeException();
        }
    }

    public String toImro() {
        var buffer = new ByteArrayOutputStream();
        try {
            Imro.write(new PrintStream(buffer), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return buffer.toString();
    }

    public void toImro(Path file) throws IOException {
        var filename = file.getFileName().toString();
        if (filename.endsWith(".imro")) {
            Imro.write(file, this);
        } else {
            throw new RuntimeException();
        }
    }

    public NpxProbeType type() {
        return type;
    }

    public @Nullable NpxMeta getMeta() {
        return meta;
    }

    public void setMeta(@Nullable NpxMeta meta) {
        this.meta = meta;
    }

    public List<@Nullable Electrode> channels() {
        return Arrays.asList(electrodes);
    }

    /**
     * {@return number of channels set}
     */
    public int size() {
        int count = 0;
        for (Electrode electrode : electrodes) {
            if (electrode != null) count++;
        }
        return count;
    }

    /**
     * @param e electrode.
     * @return Is {@code e} in this map?
     */
    public boolean contains(@Nullable Electrode e) {
        for (Electrode electrode : electrodes) {
            if (Objects.equals(electrode, e)) return true;
        }
        return false;
    }

    /**
     * {@return number of shanks}
     */
    public int nShank() {
        return type.nShank();
    }

    /**
     * {@return number of columns per shank}
     */
    public int nColumnPerShank() {
        return type.nColumnPerShank();
    }

    /**
     * {@return number of rows per shank}
     */
    public int nRowPerShank() {
        return type.nRowPerShank();
    }

    /**
     * {@return number of electrodes per shank}
     */
    public int nElectrodePerShank() {
        return type.nElectrodePerShank();
    }

    /**
     * {@return number of total channels}
     */
    public int nChannel() {
        return type.nChannel();
    }

    /**
     * {@return number of electrode blocks}
     */
    public int nElectrodePerBlock() {
        return type.nElectrodePerBlock();
    }

    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        if (reference < 0 || reference >= ReferenceInfo.maxReferenceValue(type)) {
            throw new IllegalArgumentException("illegal reference value: " + reference);
        }

        this.reference = reference;
    }

    public ReferenceInfo getReferenceInfo() {
        return ReferenceInfo.of(type, reference);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ChannelMap that)) return false;

        return reference == that.reference && type.equals(that.type) && Arrays.equals(electrodes, that.electrodes);
    }

    public int[] channelShank() {
        return ChannelMapUtil.channelShank(this, false);
    }

    public int[] channelPosX() {
        return ChannelMapUtil.channelPosX(this, false);
    }

    public int[] channelPosY() {
        return ChannelMapUtil.channelPosY(this, false);
    }

    public @Nullable Electrode getChannel(int channel) {
        return electrodes[channel];
    }

    public List<@Nullable Electrode> getChannels(int[] channel) {
        return IntStream.of(channel).mapToObj(e -> electrodes[e]).toList();
    }

    public @Nullable Electrode getElectrode(int electrode) {
        var cr = ChannelMapUtil.e2cr(type, electrode);
        return getElectrode(0, cr.c(), cr.r());
    }

    public @Nullable Electrode getElectrode(int shank, int electrode) {
        var cr = ChannelMapUtil.e2cr(type, electrode);
        return getElectrode(shank, cr.c(), cr.r());
    }

    public @Nullable Electrode getElectrode(int shank, int column, int row) {
        for (var electrode : electrodes) {
            if (electrode != null && electrode.shank == shank && electrode.column == column && electrode.row == row) {
                return electrode;
            }
        }
        return null;
    }

    public @Nullable Electrode getElectrode(Electrode electrode) {
        return getElectrode(electrode.shank, electrode.column, electrode.row);
    }

    public @Nullable Electrode getElectrode(int shank, Electrode electrode) {
        return getElectrode(shank, electrode.column, electrode.row);
    }

    public List<@Nullable Electrode> getElectrodes() {
        return Arrays.asList(electrodes);
    }

    public List<Electrode> getElectrodes(Predicate<Electrode> selector) {
        var ret = new ArrayList<Electrode>();
        for (var electrode : electrodes) {
            if (electrode != null && selector.test(electrode)) {
                ret.add(electrode);
            }
        }
        return ret;
    }

    @Override
    public Iterator<@Nullable Electrode> iterator() {
        return Arrays.asList(electrodes).iterator();
    }

    @Override
    public void forEach(Consumer<? super @Nullable Electrode> action) {
        Arrays.asList(electrodes).forEach(action);
    }

    public int[] getDisconnectChannels() {
        return IntStream.range(0, nChannel())
          .filter(i -> electrodes[i] == null)
          .toArray();
    }

    public synchronized Electrode addElectrode(int electrode) {
        var cr = ChannelMapUtil.e2cr(type, electrode);
        return addElectrode(0, cr.c(), cr.r());
    }

    public synchronized Electrode addElectrode(int shank, int electrode) {
        var cr = ChannelMapUtil.e2cr(type, electrode);
        return addElectrode(shank, cr.c(), cr.r());
    }

    public synchronized Electrode addElectrode(int shank, int column, int row) {
        if (shank < 0 || shank >= type.nShank()) throw new IllegalArgumentException("shank value out of range: " + shank);
        if (column < 0 || column >= type.nColumnPerShank()) throw new IllegalArgumentException("column value out of range: " + column);
        if (row < 0 || row >= type.nRowPerShank()) throw new IllegalArgumentException("row value out of range: " + row);

        var e = ChannelMapUtil.cr2e(type, column, row);
        var c = ChannelMapUtil.e2c(type, shank, e);
        var x = electrodes[c];
        if (x == null) {
            var ret = new Electrode(shank, column, row);
            electrodes[c] = ret;
            return ret;
        } else if (x.shank == shank && x.column == column && x.row == row) {
            return x;
        } else {
            throw new ChannelHasBeenUsedException(this, x, shank, column, row);
        }
    }

    public synchronized Electrode addElectrode(Electrode e) {
        var ret = addElectrode(e.shank, e.column, e.row);
        ret.copyFrom(e);
        return ret;
    }

    public synchronized Electrode addElectrode(int shank, Electrode e) {
        var ret = addElectrode(shank, e.column, e.row);
        ret.copyFrom(e);
        return ret;
    }

    public synchronized @Nullable Electrode removeElectrode(int electrode) {
        var cr = ChannelMapUtil.e2cr(type, electrode);
        return removeElectrode(0, cr.c(), cr.r());
    }

    public synchronized @Nullable Electrode removeElectrode(int shank, int electrode) {
        var cr = ChannelMapUtil.e2cr(type, electrode);
        return removeElectrode(shank, cr.c(), cr.r());
    }

    public synchronized @Nullable Electrode removeElectrode(int shank, int column, int row) {
        for (int i = 0, length = electrodes.length; i < length; i++) {
            var electrode = electrodes[i];
            if (electrode != null && electrode.shank == shank && electrode.column == column && electrode.row == row) {
                electrodes[i] = null;
                return electrode;
            }
        }
        return null;
    }

    public synchronized @Nullable Electrode removeElectrode(Electrode e) {
        return removeElectrode(e.shank, e.column, e.row);
    }

    public synchronized @Nullable Electrode removeElectrode(int shank, Electrode e) {
        return removeElectrode(shank, e.column, e.row);
    }

    public synchronized List<Electrode> removeElectrodes(Predicate<Electrode> selector) {
        var ret = new ArrayList<Electrode>();
        for (int i = 0, length = electrodes.length; i < length; i++) {
            var electrode = electrodes[i];
            if (electrode != null && selector.test(electrode)) {
                ret.add(electrode);
                electrodes[i] = null;
            }
        }
        return ret;
    }

    public synchronized List<Electrode> clearElectrodes() {
        var ret = new ArrayList<Electrode>();
        for (int i = 0, length = electrodes.length; i < length; i++) {
            var electrode = electrodes[i];
            if (electrode != null) {
                ret.add(electrode);
                electrodes[i] = null;
            }
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + Arrays.hashCode(electrodes);
        result = 31 * result + reference;
        return result;
    }

    @Override
    public String toString() {
        return "ChannelMap[" + nShank() + ","
               + nColumnPerShank() + ","
               + nRowPerShank() + ","
               + nChannel() + ","
               + size() + "]";
    }
}
