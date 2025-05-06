package io.ast.jneurocarto.probe_npx.select;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.probe_npx.ChannelHasBeenUsedException;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

@NullMarked
public class WeakerElectrodeSelector implements ElectrodeSelector {

    static final class Probability {
        ElectrodeDescription electrode;
        float value;

        Probability(ElectrodeDescription electrode) {
            this.electrode = electrode;
            value = switch (electrode.category()) {
                case NpxProbeDescription.CATE_SET -> 1.0F;
                case NpxProbeDescription.CATE_FULL -> 0.9F;
                case NpxProbeDescription.CATE_HALF -> 0.8F;
                case NpxProbeDescription.CATE_QUARTER -> 0.7F;
                case NpxProbeDescription.CATE_LOW -> 0.6F;
                case NpxProbeDescription.CATE_EXCLUDED -> 0F;
                default -> 0.5F;
            };
        }

        ElectrodeDescription electrode() {
            return electrode;
        }
    }

    @Override
    public <T> T select(ProbeDescription<T> desp, T chmap, List<ElectrodeDescription> blueprint) {
        if (desp instanceof NpxProbeDescription d && chmap instanceof ChannelMap c) {
            return (T) select(d, c, blueprint);
        }
        throw new RuntimeException("unsupported ProbeDescription : " + desp.getClass().getName());
    }

    public ChannelMap select(NpxProbeDescription desp, ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        var cand = desp.allElectrodes(chmap).stream().collect(Collectors.toMap(
          Function.identity(),
          Probability::new,
          (_, _) -> {
              throw new RuntimeException("duplicated electrode");
          }
        ));
        for (var electrode : blueprint) {
            cand.get(electrode).electrode.category(electrode.category());
        }

        var preSelect = cand.values().stream()
          .filter(e -> e.electrode.category() == NpxProbeDescription.CATE_SET)
          .collect(Collectors.toMap(
            Probability::electrode,
            Function.identity(),
            (_, _) -> {
                throw new RuntimeException("duplicated electrode");
            }));

        while (!preSelect.isEmpty()) {
            var e = pickElectrode(preSelect);
            if (e != null) {
                preSelect.remove(e);
                add(desp, chmap, cand, e);
            }
        }

        selectLoop(chmap, cand);

        return buildChannelMap(chmap, cand);
    }

    private void selectLoop(ChannelMap chmap, Map<ElectrodeDescription, Probability> cand) {
        var total = chmap.nChannel();
        while (size(cand) < total) {
            var e = pickElectrodeBiased(cand);
            if (e == null) {
                break;
            }

            update(cand, e);
        }
    }

    private ChannelMap buildChannelMap(ChannelMap chmap, Map<ElectrodeDescription, Probability> cand) {
        var ret = new ChannelMap(chmap.type());
        cand.values().stream().filter(e -> e.value == 1).forEach(e -> {
            try {
                chmap.addElectrode((Electrode) e.electrode.electrode());
            } catch (ChannelHasBeenUsedException ex) {
                ex.forceAddElectrode();
            }
        });
        return ret;
    }

    private @Nullable ElectrodeDescription pickElectrode(Map<ElectrodeDescription, Probability> cand) {
        if (cand.isEmpty()) return null;
        int pick = (int) (Math.random() * cand.size());
        return cand.keySet().stream().skip(pick).findFirst().orElse(null);
    }

    private @Nullable ElectrodeDescription pickElectrodeBiased(Map<ElectrodeDescription, Probability> cand) {
        if (cand.isEmpty()) return null;
        int pick = (int) (Math.random() * cand.size());
        return cand.keySet().stream().skip(pick).findFirst().orElse(null);
    }

    private void update(Map<ElectrodeDescription, Probability> cand,
                        ElectrodeDescription electrode) {
        cand.get(electrode).value = 1;

        switch (electrode.category()) {
        case NpxProbeDescription.CATE_FULL:
            /*
             ? ? ?
             o e o
             ? ? ?
             */
            get(cand, electrode, -1, 0).ifPresent(e -> increase(cand, e));
            get(cand, electrode, 1, 0).ifPresent(e -> increase(cand, e));
            break;
        case NpxProbeDescription.CATE_HALF:
            /*
             o x o
             x e x
             o x o
             */
            get(cand, electrode, -1, 0).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 1, 0).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 0, 1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 0, -1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 1, 1).ifPresent(e -> increase(cand, e));
            get(cand, electrode, 1, -1).ifPresent(e -> increase(cand, e));
            get(cand, electrode, -1, 1).ifPresent(e -> increase(cand, e));
            get(cand, electrode, -1, -1).ifPresent(e -> increase(cand, e));
            break;
        case NpxProbeDescription.CATE_QUARTER:
            /*
             ? x ?
             x x x
             x e x
             x x x
             ? x ?
             */
            get(cand, electrode, -1, 0).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 1, 0).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, -1, -1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 0, -1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 1, -1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, -1, 1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 0, 1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 1, 1).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 0, 2).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 0, -2).ifPresent(e -> decrease(cand, e));
            get(cand, electrode, 1, 2).ifPresent(e -> increase(cand, e));
            get(cand, electrode, 1, -2).ifPresent(e -> increase(cand, e));
            get(cand, electrode, -1, 2).ifPresent(e -> increase(cand, e));
            get(cand, electrode, -1, -2).ifPresent(e -> increase(cand, e));
            break;
        case NpxProbeDescription.CATE_LOW:
        case NpxProbeDescription.CATE_UNSET:
            break;
        default:
            throw new RuntimeException("un-reachable");
        }
    }

    private int size(Map<ElectrodeDescription, Probability> cand) {
        var size = 0;
        for (var probability : cand.values()) {
            if (probability.value == 1) size++;
        }
        return size;
    }

    private void add(NpxProbeDescription desp,
                     ChannelMap chmap,
                     Map<ElectrodeDescription, Probability> cand,
                     ElectrodeDescription electrode) {
        desp.getInvalidElectrodes(chmap, electrode, cand.keySet()).forEach(e -> {
            if (!e.equals(electrode)) {
                remove(cand, electrode);
            } else {
                cand.get(electrode).value = 1;
            }
        });
    }

    private void remove(Map<ElectrodeDescription, Probability> cand,
                        ElectrodeDescription electrode) {
        cand.get(electrode).value = 0;
    }

    private void increase(Map<ElectrodeDescription, Probability> cand,
                          ElectrodeDescription electrode) {
        var p = cand.get(electrode);
        if (p.value > 0 && p.value < 1) {
            p.value = 0.95F;
        }
    }

    private void decrease(Map<ElectrodeDescription, Probability> cand,
                          ElectrodeDescription electrode) {
        var p = cand.get(electrode);
        if (p.value < 1) {
            p.value /= 2;
        }
    }

    private Optional<ElectrodeDescription> get(Map<ElectrodeDescription, Probability> cand,
                                               ElectrodeDescription electrode,
                                               int col,
                                               int row) {
        var e = (Electrode) electrode.electrode();
        var s = e.shank;
        var c = e.column + col;
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
