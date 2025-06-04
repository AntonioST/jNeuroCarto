package io.ast.jneurocarto.probe_npx;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.ShankCoordinate;
import io.ast.jneurocarto.core.numpy.Numpy;

@NullMarked
public class NpxProbeDescription implements ProbeDescription<ChannelMap> {

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

    private static final Map<Integer, String> ALL_STATES = Map.of(
        STATE_UNUSED, "Disable",
        STATE_USED, "Enable",
        STATE_DISABLED, "Forbidden"
    );

    private static final Map<Integer, String> ALL_CATEGORIES = Map.of(
        CATE_UNSET, "Unset",
        CATE_SET, "Pre Selected",
        CATE_FULL, "Full Density",
        CATE_HALF, "Half Density",
        CATE_QUARTER, "Quarter Density",
        CATE_LOW, "Low priority",
        CATE_EXCLUDED, "Excluded"
    );

    private static final List<String> STATES = List.of("Enable", "Disable");
    private static final List<String> CATEGORIES = List.of("Unset", "Pre Selected", "Full Density", "Half Density", "Quarter Density", "Low priority", "Excluded");

    @Override
    public List<String> supportedProbeType() {
        return List.of("NP0", "NP21", "NP24");
    }

    @Override
    public String probeTypeDescription(String code) {
        return switch (channelmapType(code)) {
            case NpxProbeType.NP1 _ -> "Neuropixels probe";
            case NpxProbeType.NP21 _ -> "Neuropixels probe 2.0";
            case NpxProbeType.NP24 _ -> "4-Shank Neuropixels probe 2.0";
        };
    }

    @Override
    public List<String> availableStates() {
        return STATES;
    }

    @Override
    public Map<Integer, String> allStates() {
        return ALL_STATES;
    }

    @Override
    public List<String> availableCategories() {
        return CATEGORIES;
    }

    @Override
    public Map<Integer, String> allCategories() {
        return ALL_CATEGORIES;
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

    public NpxProbeType channelmapType(String code) {
        return NpxProbeType.of(code);
    }

    public @Nullable NpxProbeType channelmapType(Object chmap) {
        if (!(chmap instanceof ChannelMap m)) return null;
        return m.type();
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
            return "" + chmap.size() + "/" + chmap.nChannel();
        }
    }

    @Override
    public List<ElectrodeDescription> allElectrodes(String code) {
        var type = NpxProbeType.of(code);
        var scr = ChannelMapUtil.electrodePosSCR(type);
        var c = ChannelMapUtil.e2c(type, scr);
        var sxy = ChannelMapUtil.e2xy(type, scr);
        var ret = new ArrayList<ElectrodeDescription>(c.length);
        for (int i = 0, length = c.length; i < length; i++) {
            ret.add(new ElectrodeDescription(
                sxy[0][i], sxy[1][i], sxy[2][i],
                new Electrode(scr[0][i], scr[1][i], scr[2][i]),
                c[i]
            ));
        }
        return ret;
    }

    @Override
    public List<ElectrodeDescription> allChannels(ChannelMap chmap) {
        var type = chmap.type();
        var ret = new ArrayList<ElectrodeDescription>(chmap.size());
        for (var electrode : chmap) {
            if (electrode != null) {
                var sxy = ChannelMapUtil.e2xy(type, electrode);
                var c = ChannelMapUtil.e2c(type, electrode);
                ret.add(new ElectrodeDescription(
                    sxy.s(), sxy.x(), sxy.y(),
                    electrode, c
                ));
            }
        }
        return ret;
    }

    @Override
    public List<ElectrodeDescription> allChannels(ChannelMap chmap, Collection<ElectrodeDescription> electrodes) {
        var ret = new ArrayList<ElectrodeDescription>(chmap.size());
        for (var electrode : chmap) {
            if (electrode != null) {
                getElectrode(electrodes, electrode).ifPresent(ret::add);
            }
        }
        return ret;
    }

    @Override
    public boolean validateChannelmap(ChannelMap chmap) {
        return chmap.size() == chmap.nChannel();
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
        return new ElectrodeDescription(r.shank, xy.x(), xy.y(), r, c);
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
    public ElectrodeDescription copyElectrode(ElectrodeDescription e) {
        return new ElectrodeDescription(
            e.s(), e.x(), e.y(),
            new Electrode((Electrode) e.electrode()),
            e.channel(),
            e.state(), e.category()
        );
    }

    @Override
    public boolean isElectrodeCompatible(ChannelMap chmap, ElectrodeDescription e1, ElectrodeDescription e2) {
        return ((int) e1.channel()) != ((int) e2.channel());
    }

    @Override
    public List<ElectrodeDescription> loadBlueprint(Path file) throws IOException {
        var filename = file.getFileName().toString();
        if (!filename.endsWith(".npy")) throw new IllegalArgumentException("not a .npy filename : " + filename);

        // int[C][R] array that {0: shank, 1: column, 2: row, 3: state, 4: category}
        var data = Numpy.read(file, Numpy.ofD2Int(true));
        if (data.length != 5) throw new IllegalArgumentException("bad blueprint file format");

        var length = data[0].length;

        for (var code : supportedProbeType()) {
            if (NpxProbeType.of(code).nElectrode() == length) {
                return readBlueprint(data, allElectrodes(code));
            }
        }

        throw new RuntimeException("cannot not found match probe type.");
    }

    @Override
    public List<ElectrodeDescription> loadBlueprint(Path file, ChannelMap chmap) throws IOException {
        var filename = file.getFileName().toString();
        if (!filename.endsWith(".npy")) throw new IllegalArgumentException("not a .npy filename : " + filename);

        // int[5][N] array that {0: shank, 1: column, 2: row, 3: state, 4: category}
        var data = Numpy.read(file, Numpy.ofD2Int(true));
        if (data.length != 5) throw new IllegalArgumentException("bad blueprint file format");

        return readBlueprint(data, allElectrodes(chmap));
    }

    private List<ElectrodeDescription> readBlueprint(int[][] data, List<ElectrodeDescription> ret) throws IOException {
        var length = data[0].length;
        for (int i = 0; i < length; i++) {
            var s = data[0][i];
            var c = data[1][i];
            var r = data[2][i];

            var j = i;
            getElectrode(ret, new Electrode(s, c, r)).ifPresent(e -> {
                e.state(data[3][j]);
                e.category(data[4][j]);
            });
        }

        return ret;
    }

    @Override
    public void saveBlueprint(Path file, List<ElectrodeDescription> electrodes) throws IOException {
        var filename = file.getFileName().toString();
        if (!filename.endsWith(".npy")) throw new IllegalArgumentException("not a .npy filename : " + filename);

        var length = electrodes.size();

        var ret = new int[5][];
        for (int i = 0; i < 5; i++) {
            ret[i] = new int[length];
        }

        for (int i = 0; i < length; i++) {
            var e = electrodes.get(i);
            var t = (Electrode) e.electrode();
            ret[0][i] = t.shank;
            ret[1][i] = t.column;
            ret[2][i] = t.row;
            ret[3][i] = e.state();
            ret[4][i] = e.category();
        }

        Numpy.write(file, ret, Numpy.ofD2Int(true));
    }

    @Override
    public ShankCoordinate getShankCoordinate(String code) {
        return switch (channelmapType(code)) {
            case null -> throw new IllegalArgumentException("unknown channelmap code");
            case NpxProbeType.NP24 t -> ShankCoordinate.linear(t.spacePerShank());
            default -> ShankCoordinate.ZERO;
        };
    }
}
