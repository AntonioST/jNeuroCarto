package io.ast.jneurocarto.probe_npx;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public class NeuropixelsProbeDescription implements ProbeDescription<ChannelMap> {

    /**
     * electrode full-density category.
     */
    public static final int CATE_FULL = 11;

    /**
     * electrode half-density category
     */
    public static final int CATE_HALF = 12;

    /**
     * electrode quarter-density category.
     */
    public static final int CATE_QUARTER = 13;

    @Override
    public List<String> supportedProbeType() {
        return List.of("NP0", "NP21", "NP24");
    }

    @Override
    public String probeTypeDescription(String code) {
        return switch (code) {
            case "NP0" -> "Neuropixels probe";
            case "NP21" -> "Neuropixels probe 2.0";
            case "NP24" -> "4-Shank Neuropixels probe 2.0";
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public List<String> availableStates() {
        return List.of("Enable", "Disable");
    }

    @Override
    public List<String> availableCategories() {
        return List.of("Unset", "Pre Selected", "Full Density", "Half Density", "Quarter Density", "Low priority", "Excluded");
    }

    @Override
    public List<String> channelMapFileSuffix() {
        return List.of(".imro", ".meta");
    }

    @Override
    public ChannelMap load(Path file) throws IOException {
        return ChannelMap.fromFile(file);
    }

    @Override
    public void save(Path file, ChannelMap chmap) throws IOException {
        chmap.toImro(file);
    }

    @Override
    public @Nullable String channelmapCode(Object chmap) {
        if (!(chmap instanceof ChannelMap m)) return null;
        return switch (m.type()) {
            case NpxProbeType.NP1 _ -> "NP0";
            case NpxProbeType.NP21 _ -> "NP21";
            case NpxProbeType.NP24 _ -> "NP24";
        };
    }

    @Override
    public ChannelMap newChannelmap(String code) {
        return new ChannelMap(NpxProbeType.of(code));
    }

    @Override
    public ChannelMap newChannelmap(ChannelMap chmap) {
        return new ChannelMap(chmap.type());
    }

    @Override
    public ChannelMap copyChannelmap(ChannelMap chmap) {
        return new ChannelMap(chmap);
    }

    @Override
    public String despChannelmap(@Nullable ChannelMap chmap) {
        if (chmap == null) {
            return "0/0";
        } else {
            return "" + chmap.length() + "/" + chmap.nChannel();
        }
    }

    @Override
    public List<ElectrodeDescription> allElectrodes(String code) {
        //XXX Unsupported Operation NeuropixelsProbeDescription.allElectrodes
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ElectrodeDescription> allChannels(ChannelMap chmap) {
        //XXX Unsupported Operation NeuropixelsProbeDescription.allChannels
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ElectrodeDescription> allChannels(ChannelMap chmap, List<ElectrodeDescription> subset) {
        //XXX Unsupported Operation NeuropixelsProbeDescription.allChannels
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean validateChannelmap(ChannelMap chmap) {
        return chmap.length() == chmap.nChannel();
    }

    @Override
    public @Nullable ElectrodeDescription addElectrode(ChannelMap chmap, ElectrodeDescription e, boolean force) {
        Electrode r = null;
        try {
            r = chmap.addElectrode((Electrode) e.electrode());
        } catch (ChannelHasBeenUsedException ex) {
            if (force) {
                r = ex.forceAddElectrode();
            }
        }

        return r == null ? null : asDesp(chmap.type(), r);
    }

    private static ElectrodeDescription asDesp(NpxProbeType t, Electrode r) {
        var xy = ChannelMapUtil.e2xy(t, r);
        var c = ChannelMapUtil.e2c(t, r);
        return new ElectrodeDescription(r.shank, xy.x(), xy.y(), r, c, STATE_UNUSED, CATE_UNSET);
    }

    @Override
    public boolean removeElectrode(ChannelMap chmap, ElectrodeDescription e, boolean force) {
        return chmap.removeElectrode((Electrode) e.electrode()) != null;
    }

    @Override
    public List<ElectrodeDescription> clearElectrodes(ChannelMap chmap) {
        var t = chmap.type();
        return chmap.clearElectrodes().stream().map(e -> asDesp(t, e)).toList();
    }

    @Override
    public List<ElectrodeDescription> copyElectrodes(List<ElectrodeDescription> electrodes) {
        //XXX Unsupported Operation NeuropixelsProbeDescription.copyElectrodes
        throw new UnsupportedOperationException();
    }
}
