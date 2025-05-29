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
@ElectrodeSelector.Selector("default")
@RequestChannelmap(probe = NpxProbeDescription.class)
public class DefaultElectrodeSelector implements ElectrodeSelector {

    private static final int CATE_INVALIDED = Integer.MAX_VALUE;

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

        Selector(Blueprint<ChannelMap> blueprint) {
            var chmap = blueprint.channelmap();
            type = chmap.type();
            this.blueprint = blueprint;
            tool = new BlueprintToolkit<>(blueprint);
            electrodes = blueprint.electrodes();
        }

        public ChannelMap select() {
            var ret = blueprint.newChannelmap();

            tool.mask(ProbeDescription.CATE_SET).forEach(i -> add(ret, i));
            tool.setTo(ProbeDescription.CATE_EXCLUDED, CATE_INVALIDED);

            return selectLoop(ret);
        }

        private ChannelMap selectLoop(ChannelMap chmap) {
            while (tool.count(CATE_INVALIDED) < tool.length()) {
                var e = pickElectrode();
                if (e >= 0) {
                    update(chmap, e);
                } else {
                    break;
                }
            }
            return chmap;
        }

        private int pickElectrode() {
            int ret;
            if ((ret = pickElectrode(NpxProbeDescription.CATE_FULL)) >= 0) return ret;
            if ((ret = pickElectrode(NpxProbeDescription.CATE_HALF)) >= 0) return ret;
            if ((ret = pickElectrode(NpxProbeDescription.CATE_QUARTER)) >= 0) return ret;
            if ((ret = pickElectrode(NpxProbeDescription.CATE_LOW)) >= 0) return ret;
            if ((ret = pickElectrode(NpxProbeDescription.CATE_UNSET)) >= 0) return ret;
            return -1;
        }

        private int pickElectrode(int category) {
            var mask = tool.mask(category);
            var count = mask.count();
            if (count == 0) return -1;
            int pick = (int) (Math.random() * count);
            return mask.getSet(pick);
        }

        private void update(ChannelMap chmap, int e) {
            switch (tool.category(e)) {
            case NpxProbeDescription.CATE_FULL -> updateD1(chmap, e);
            case NpxProbeDescription.CATE_HALF -> updateD2(chmap, e);
            case NpxProbeDescription.CATE_QUARTER -> updateD4(chmap, e);
            case NpxProbeDescription.CATE_LOW, NpxProbeDescription.CATE_UNSET -> add(chmap, e);
            default -> throw new RuntimeException("un-reachable");
            }
        }

        private void updateD1(ChannelMap chmap, int e) {
            if (e < 0 || tool.category(e) == CATE_INVALIDED) return;
            add(chmap, e);

            add(chmap, get(e, 1, 0));
            updateD1(chmap, get(e, 0, 1));
            updateD1(chmap, get(e, 0, -1));
        }

        private void updateD2(ChannelMap chmap, int e) {
            if (e < 0 || tool.category(e) == CATE_INVALIDED) return;
            add(chmap, e);

            remove(get(e, 1, 0));
            remove(get(e, 0, 1));
            remove(get(e, 0, -1));
            updateD2(chmap, get(e, 1, 1));
            updateD2(chmap, get(e, 1, -1));
        }

        private void updateD4(ChannelMap chmap, int e) {
            if (e < 0 || tool.category(e) == CATE_INVALIDED) return;
            add(chmap, e);

            remove(get(e, 1, 0));
            remove(get(e, 0, 1));
            remove(get(e, 1, 1));
            remove(get(e, 0, -1));
            remove(get(e, 1, -1));
            remove(get(e, 0, 2));
            remove(get(e, 0, -2));
            updateD4(chmap, get(e, 1, 2));
            updateD4(chmap, get(e, 1, -2));
        }

        private void add(ChannelMap chmap, int e) {
            if (e >= 0) {
                var electrode = electrodes.get(e);
                var added = tool.probe().addElectrode(chmap, electrode);
                tool.set(CATE_INVALIDED, e);
                if (added != null) {
                    tool.set(CATE_INVALIDED, tool.invalid(electrodes, electrode));
                }
            }
        }

        private void remove(int remove) {
            if (remove >= 0) tool.set(CATE_INVALIDED, remove);
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
