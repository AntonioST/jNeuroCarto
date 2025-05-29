package io.ast.jneurocarto.probe_npx;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.RequestChannelmap;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;

@NullMarked
@ElectrodeSelector.Selector("weaker")
@RequestChannelmap(probe = NpxProbeDescription.class)
public class WeakerElectrodeSelector implements ElectrodeSelector {

    @Override
    public <T> T select(Blueprint<T> blueprint) {
        var _ = (ChannelMap) blueprint.channelmap();
        return (T) new Selector((Blueprint<ChannelMap>) blueprint).select();
    }

    private static class Selector {
        private final NpxProbeType type;
        private final Blueprint<ChannelMap> blueprint;
        private final BlueprintToolkit<ChannelMap> tool;
        private final List<ElectrodeDescription> electrodes;
        private final int[] probability; // probability as rounded percentage.

        Selector(Blueprint<ChannelMap> blueprint) {
            var chmap = blueprint.channelmap();
            type = chmap.type();
            this.blueprint = blueprint;
            tool = new BlueprintToolkit<>(blueprint);
            electrodes = blueprint.electrodes();
            probability = new int[tool.length()];
        }

        public ChannelMap select() {
            var ret = blueprint.newChannelmap();

            for (int i = 0, length = probability.length; i < length; i++) {
                probability[i] = switch (tool.category(i)) {
                    case NpxProbeDescription.CATE_SET -> 100;
                    case NpxProbeDescription.CATE_FULL -> 90;
                    case NpxProbeDescription.CATE_HALF -> 80;
                    case NpxProbeDescription.CATE_QUARTER -> 70;
                    case NpxProbeDescription.CATE_LOW -> 60;
                    case NpxProbeDescription.CATE_EXCLUDED -> 0;
                    default -> 50;
                };

            }

            tool.mask(ProbeDescription.CATE_SET).forEach(this::add);

            selectLoop();

            return buildChannelMap();
        }

        private ChannelMap buildChannelMap() {
            var ret = blueprint.newChannelmap();
            for (int i = 0, length = probability.length; i < length; i++) {
                if (probability[i] == 100) {
                    try {
                        ret.addElectrode((Electrode) electrodes.get(i).electrode());
                    } catch (ChannelHasBeenUsedException ex) {
                        ex.forceAddElectrode();
                    }
                }
            }
            return ret;
        }

        private void selectLoop() {
            var total = type.nChannel();
            while (size() < total) {
                var e = pickElectrode();
                if (e < 0) {
                    break;
                }

                update(e);
            }
        }

        private int size() {
            var ret = 0;
            for (double v : probability) {
                if (v == 100) ret++;
            }
            return ret;
        }

        private int high() {
            var ret = 0;
            for (var p : probability) {
                if (p < 100) ret = Math.max(ret, p);
            }
            return ret;
        }

        private int count(int p) {
            var ret = 0;
            for (var q : probability) {
                if (q >= p && q < 100) ret++;
            }
            return ret;
        }

        private int pick(int p, int n) {
            for (int i = 0, j = 0, length = probability.length; i < length; i++) {
                var q = probability[i];
                if (q >= p && q < 100) {
                    if (j == n) return i;
                    j++;
                }
            }
            return -1;
        }

        private int pickElectrode() {
            var h = high();
            if (h == 0) return -1;
            var c = count(h);
            var p = (int) (Math.random() * c);
            return pick(h, p);
        }

        private void update(int i) {
            if (i < 0) return;
            probability[i] = 100;

            switch (tool.category(i)) {
            case NpxProbeDescription.CATE_FULL:
                /*
                 ? ? ?
                 o e o
                 ? ? ?
                 */
                increase(get(i, -1, 0));
                increase(get(i, 1, 0));
                break;
            case NpxProbeDescription.CATE_HALF:
                /*
                 o x o
                 x e x
                 o x o
                 */
                decrease(get(i, -1, 0));
                decrease(get(i, 1, 0));
                decrease(get(i, 0, 1));
                decrease(get(i, 0, -1));

                increase(get(i, 1, 1));
                increase(get(i, 1, -1));
                increase(get(i, -1, 1));
                increase(get(i, -1, -1));
                break;
            case NpxProbeDescription.CATE_QUARTER:
                /*
                 ? x ?
                 x x x
                 x e x
                 x x x
                 ? x ?
                 */
                decrease(get(i, -1, 0));
                decrease(get(i, 1, 0));
                decrease(get(i, -1, -1));
                decrease(get(i, 0, -1));
                decrease(get(i, 1, -1));
                decrease(get(i, -1, 1));
                decrease(get(i, 0, 1));
                decrease(get(i, 1, 1));
                decrease(get(i, 0, 2));
                decrease(get(i, 0, -2));

                increase(get(i, 1, 2));
                increase(get(i, 1, -2));
                increase(get(i, -1, 2));
                increase(get(i, -1, -2));
                break;
            case NpxProbeDescription.CATE_LOW:
            case NpxProbeDescription.CATE_UNSET:
                break;
            default:
                throw new RuntimeException("un-reachable");
            }
        }

        private void add(int i) {
            if (i >= 0) {
                BlueprintToolkit.set(probability, tool.invalid(electrodes, i), 0);
                probability[i] = 100;
            }
        }

        private void remove(int i) {
            if (i >= 0) probability[i] = 0;
        }

        private void increase(int i) {
            if (i >= 0) {
                var p = probability[i];
                if (p > 0 && p < 100) {
                    probability[i] = 95;
                }
            }
        }

        private void decrease(int i) {
            if (i >= 0) {
                var p = probability[i];
                if (p < 1) {
                    probability[i] = p / 2;
                }
            }
        }

        private int get(int i, int col, int row) {
            var n = type.nColumnPerShank();
            var e = (Electrode) electrodes.get(i).electrode();
            var s = e.shank;
            var c = (e.column + col) % n;
            var r = e.row + row;

            var x = s * type.spacePerShank() + c * type.spacePerColumn();
            var y = r * type.spacePerRow();

            var t = tool.index(s, x, y);
            if (t >= 0 && tool.category(i) == tool.category(t)) return t;
            return -1;
        }

    }
}
