package io.ast.jneurocarto.probe_npx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ChannelMap {

    private final NpxProbeInfo info;
    private final @Nullable Electrode[] electrodes;
    private int reference = 0;
    private @Nullable NpxMeta meta = null;

    public ChannelMap(NpxProbeType type) {
        this(type.info());
    }

    public ChannelMap(NpxProbeInfo info) {
        this.info = info;
        electrodes = new Electrode[info.nChannel()];
    }

    public ChannelMap(ChannelMap map) {
        this(map.info, map.channels(), map.meta);
    }

    ChannelMap(NpxProbeInfo info, List<@Nullable Electrode> electrodes, @Nullable NpxMeta meta) {
        this(info);
    }

    public NpxProbeInfo info() {
        return info;
    }

    public List<@Nullable Electrode> channels() {
        return Arrays.asList(electrodes);
    }

    /**
     * {@return number of channels set}
     */
    public int length() {
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
        return info.nShank();
    }

    /**
     * {@return number of columns per shank}
     */
    public int nColumnPerShank() {
        return info.nColumnPerShank();
    }

    /**
     * {@return number of rows per shank}
     */
    public int nRowPerShank() {
        return info.nRowPerShank();
    }

    /**
     * {@return number of electrodes per shank}
     */
    public int nElectrodePerShank() {
        return info.nElectrodePerShank();
    }

    /**
     * {@return number of total channels}
     */
    public int nChannel() {
        return info.nChannel();
    }

    /**
     * {@return number of electrode blocks}
     */
    public int nElectrodePerBlock() {
        return info.nElectrodePerBlock();
    }

    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        if (reference < 0 || reference >= ReferenceInfo.maxReferenceValue(info)) {
            throw new IllegalArgumentException("illegal reference value: " + reference);
        }

        this.reference = reference;
    }

    public ReferenceInfo getReferenceInfo() {
        return ReferenceInfo.of(info, reference);
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof ChannelMap that)) return false;

        return reference == that.reference && info.equals(that.info) && Arrays.equals(electrodes, that.electrodes);
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
        //XXX Unsupported Operation ChannelMap.getElectrode
        throw new UnsupportedOperationException();
    }

    public @Nullable Electrode getElectrode(int shank, int electrode) {
        //XXX Unsupported Operation ChannelMap.getElectrode
        throw new UnsupportedOperationException();
    }

    public @Nullable Electrode getElectrode(int shank, int column, int row) {
        for (var electrode : electrodes) {
            if (electrode != null && electrode.shank() == shank && electrode.column() == column && electrode.row() == row) {
                return electrode;
            }
        }
        return null;
    }

    public @Nullable Electrode getElectrode(Electrode electrode) {
        return getElectrode(electrode.shank(), electrode.column(), electrode.row());
    }

    public @Nullable Electrode getElectrode(int shank, Electrode electrode) {
        return getElectrode(shank, electrode.column(), electrode.row());
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

    public int[] getDisconnectChannels() {
        return IntStream.range(0, nChannel())
          .filter(i -> electrodes[i] == null)
          .toArray();
    }

    public synchronized Electrode addElectrode(int electrode) {
        var cr = ChannelMapUtil.e2cr(info, electrode);
        return addElectrode(0, cr.c(), cr.r());
    }

    public synchronized Electrode addElectrode(int shank, int electrode) {
        var cr = ChannelMapUtil.e2cr(info, electrode);
        return addElectrode(shank, cr.c(), cr.r());
    }

    public synchronized Electrode addElectrode(int shank, int column, int row) {
        if (shank < 0 || shank >= info.nShank()) throw new IllegalArgumentException("shank value out of range: " + shank);
        if (column < 0 || column >= info.nColumnPerShank()) throw new IllegalArgumentException("column value out of range: " + column);
        if (row < 0 || row >= info.nRowPerShank()) throw new IllegalArgumentException("row value out of range: " + row);

        var e = ChannelMapUtil.cr2e(info, column, row);
        var c = ChannelMapUtil.e2c(info, shank, e);
        var x = electrodes[c];
        if (x == null) {
            var ret = new Electrode(shank, column, row);
            electrodes[c] = ret;
            return ret;
        } else if (x.shank() == shank && x.column() == column && x.row() == row) {
            return x;
        } else {
            throw new ChannelHasBeenUsedException(this, x, shank, column, row);
        }
    }

    public synchronized Electrode addElectrode(Electrode e) {
        return addElectrode(e.shank(), e.column(), e.row());
    }

    public synchronized Electrode addElectrode(int shank, Electrode e) {
        return addElectrode(shank, e.column(), e.row());
    }

    public synchronized @Nullable Electrode removeElectrode(int electrode) {
        var cr = ChannelMapUtil.e2cr(info, electrode);
        return removeElectrode(0, cr.c(), cr.r());
    }

    public synchronized @Nullable Electrode removeElectrode(int shank, int electrode) {
        var cr = ChannelMapUtil.e2cr(info, electrode);
        return removeElectrode(shank, cr.c(), cr.r());
    }

    public synchronized @Nullable Electrode removeElectrode(int shank, int column, int row) {
        for (int i = 0, length = electrodes.length; i < length; i++) {
            var electrode = electrodes[i];
            if (electrode != null && electrode.shank() == shank && electrode.column() == column && electrode.row() == row) {
                electrodes[i] = null;
                return electrode;
            }
        }
        return null;
    }

    public synchronized @Nullable Electrode removeElectrode(Electrode e) {
        return removeElectrode(e.shank(), e.column(), e.row());
    }

    public synchronized @Nullable Electrode removeElectrode(int shank, Electrode e) {
        return removeElectrode(shank, e.column(), e.row());
    }

    @Override
    public int hashCode() {
        int result = info.hashCode();
        result = 31 * result + Arrays.hashCode(electrodes);
        result = 31 * result + reference;
        return result;
    }

    @Override
    public String toString() {
        return "ChannelMap[";
    }
}
