package io.ast.jneurocarto.core;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;

@NullMarked
@ElectrodeSelector.Selector("random")
public class RandomElectrodeSelector implements ElectrodeSelector {

    private static final int CATE_INVALIDED = Integer.MAX_VALUE;
    private boolean ignorePreSelected = false;
    private boolean ignoreExclude = false;

    @Override
    public Map<String, String> getOptions() {
        return Map.of(
          "ignore_preselected", ignorePreSelected ? "1" : "0",
          "ignore_exclude", ignoreExclude ? "1" : "0"
        );
    }

    @Override
    public void setOption(String name, String value) {
        switch (name) {
        case "ignore_preselected":
            ignorePreSelected = Integer.parseInt(value) != 0;
            break;
        case "ignore_exclude":
            ignoreExclude = Integer.parseInt(value) != 0;
            break;
        }
    }

    @Override
    public <T> T select(Blueprint<T> blueprint) {
        var tool = new BlueprintToolkit<>(blueprint);
        var ret = blueprint.newChannelmap();
        var electrodes = blueprint.electrodes();

        if (!ignorePreSelected) {
            tool.mask(ProbeDescription.CATE_SET).forEach(i -> add(tool, ret, electrodes, i));
        }

        return selectLoop(tool, ret, electrodes);
    }

    private <T> T selectLoop(BlueprintToolkit<T> tool, T chmap, List<ElectrodeDescription> electrodes) {
        while (tool.count(CATE_INVALIDED) < tool.length()) {
            var e = pickElectrode(tool);
            if (e >= 0) {
                add(tool, chmap, electrodes, e);
            }
        }
        return chmap;
    }

    private int pickElectrode(BlueprintToolkit<?> tool) {
        var valid = tool.mask(e -> e.c() != CATE_INVALIDED);
        if (!ignoreExclude) {
            valid = valid.diff(tool.mask(ProbeDescription.CATE_EXCLUDED));
        }

        var count = valid.count();
        if (count == 0) return -1;
        int pick = (int) (Math.random() * count);
        return valid.getSet(pick);
    }

    private <T> void add(BlueprintToolkit<T> tool, T chmap, List<ElectrodeDescription> electrodes, int add) {
        var e = electrodes.get(add);
        var added = tool.probe().addElectrode(chmap, e);
        tool.set(CATE_INVALIDED, add);
        if (added != null) {
            tool.set(CATE_INVALIDED, tool.invalid(electrodes, e));
        }
    }
}
