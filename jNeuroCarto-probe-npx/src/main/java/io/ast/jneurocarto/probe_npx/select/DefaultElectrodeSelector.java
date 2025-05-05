package io.ast.jneurocarto.probe_npx.select;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

@NullMarked
public class DefaultElectrodeSelector implements ElectrodeSelector<NpxProbeDescription, ChannelMap> {

    @Override
    public ChannelMap select(NpxProbeDescription desp, ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        var ret = desp.newChannelmap(chmap);
        var cand = desp.allElectrodes(chmap).stream().collect(Collectors.toMap(
          e -> e,
          e -> e,
          (_, _) -> {
              throw new RuntimeException("duplicated electrode");
          }
        ));
        for (var electrode : blueprint) {
            cand.get(electrode).category(electrode.category());
        }

        for (var electrode : blueprint) {
            switch (electrode.category()) {
            case NpxProbeDescription.CATE_SET -> add(desp, ret, cand, electrode);
            case NpxProbeDescription.CATE_EXCLUDED -> cand.remove(electrode);
            }
        }

        return selectLoop(desp, ret, cand);

    }

    private ChannelMap selectLoop(NpxProbeDescription desp, ChannelMap chmap, Map<ElectrodeDescription, ElectrodeDescription> cand) {
        while (!cand.isEmpty()) {
            var e = pickElectrode(cand);
            if (e == null || e.category() == NpxProbeDescription.CATE_EXCLUDED) {
                break;
            }
            update(desp, chmap, cand, e);
        }
        return chmap;
    }

    private @Nullable ElectrodeDescription pickElectrode(Map<ElectrodeDescription, ElectrodeDescription> cand) {
        if (cand.isEmpty()) return null;
        ElectrodeDescription ret;
        if ((ret = pickElectrode(cand, NpxProbeDescription.CATE_FULL)) != null) return ret;
        if ((ret = pickElectrode(cand, NpxProbeDescription.CATE_HALF)) != null) return ret;
        if ((ret = pickElectrode(cand, NpxProbeDescription.CATE_QUARTER)) != null) return ret;
        if ((ret = pickElectrode(cand, NpxProbeDescription.CATE_LOW)) != null) return ret;
        if ((ret = pickElectrode(cand, NpxProbeDescription.CATE_UNSET)) != null) return ret;
        return null;
    }

    private @Nullable ElectrodeDescription pickElectrode(Map<ElectrodeDescription, ElectrodeDescription> cand, int category) {
        var found = cand.keySet().stream().filter(e -> e.category() == category).toList();
        if (found.isEmpty()) return null;
        int pick = (int) (Math.random() * found.size());
        return found.get(pick);
    }

    private void update(NpxProbeDescription desp,
                        ChannelMap chmap,
                        Map<ElectrodeDescription, ElectrodeDescription> cand,
                        ElectrodeDescription electrode) {
        switch (electrode.category()) {
        case NpxProbeDescription.CATE_FULL -> updateD1(desp, chmap, cand, electrode);
        case NpxProbeDescription.CATE_HALF -> updateD2(desp, chmap, cand, electrode);
        case NpxProbeDescription.CATE_QUARTER -> updateD4(desp, chmap, cand, electrode);
        case NpxProbeDescription.CATE_LOW, NpxProbeDescription.CATE_UNSET -> add(desp, chmap, cand, electrode);
        default -> throw new RuntimeException("un-reachable");
        }
    }

    private void updateD1(NpxProbeDescription desp,
                          ChannelMap chmap,
                          Map<ElectrodeDescription, ElectrodeDescription> cand,
                          ElectrodeDescription electrode) {
        add(desp, chmap, cand, electrode);

        get(chmap, cand, electrode, 1, 0).ifPresent(e -> add(desp, chmap, cand, e));
        get(chmap, cand, electrode, 0, 1).ifPresent(e -> updateD1(desp, chmap, cand, e));
        get(chmap, cand, electrode, 0, -1).ifPresent(e -> updateD1(desp, chmap, cand, e));

    }

    private void updateD2(NpxProbeDescription desp,
                          ChannelMap chmap,
                          Map<ElectrodeDescription, ElectrodeDescription> cand,
                          ElectrodeDescription electrode) {
        add(desp, chmap, cand, electrode);

        get(chmap, cand, electrode, 1, 0).ifPresent(cand::remove);
        get(chmap, cand, electrode, 0, 1).ifPresent(cand::remove);
        get(chmap, cand, electrode, 0, -1).ifPresent(cand::remove);
        get(chmap, cand, electrode, 1, 1).ifPresent(e -> updateD2(desp, chmap, cand, e));
        get(chmap, cand, electrode, 1, -1).ifPresent(e -> updateD2(desp, chmap, cand, e));
    }

    private void updateD4(NpxProbeDescription desp,
                          ChannelMap chmap,
                          Map<ElectrodeDescription, ElectrodeDescription> cand,
                          ElectrodeDescription electrode) {
        add(desp, chmap, cand, electrode);

        get(chmap, cand, electrode, 1, 0).ifPresent(cand::remove);
        get(chmap, cand, electrode, 0, 1).ifPresent(cand::remove);
        get(chmap, cand, electrode, 1, 1).ifPresent(cand::remove);
        get(chmap, cand, electrode, 0, -1).ifPresent(cand::remove);
        get(chmap, cand, electrode, 1, -1).ifPresent(cand::remove);
        get(chmap, cand, electrode, 0, 2).ifPresent(cand::remove);
        get(chmap, cand, electrode, 0, -2).ifPresent(cand::remove);
        get(chmap, cand, electrode, 1, 2).ifPresent(e -> updateD4(desp, chmap, cand, e));
        get(chmap, cand, electrode, 1, -2).ifPresent(e -> updateD4(desp, chmap, cand, e));
    }

    private void add(NpxProbeDescription desp,
                     ChannelMap chmap,
                     Map<ElectrodeDescription, ElectrodeDescription> cand,
                     ElectrodeDescription electrode) {
        var added = desp.addElectrode(chmap, electrode);
        if (added != null) {
            desp.getInvalidElectrodes(chmap, added, cand.keySet()).forEach(cand::remove);
        }
    }

    private Optional<ElectrodeDescription> get(ChannelMap chmap,
                                               Map<ElectrodeDescription, ElectrodeDescription> cand,
                                               ElectrodeDescription electrode,
                                               int col,
                                               int row) {
        var type = chmap.type();
        var n = type.nColumnPerShank();
        var e = (Electrode) electrode.electrode();
        var s = e.shank;
        var c = (e.column + col) % n;
        var r = e.row + row;
        var g = electrode.category();

        for (var k : cand.keySet()) {
            var t = (Electrode) k.electrode();
            if (t.shank == s && t.column == c && t.row == r && k.category() == g) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }
}
