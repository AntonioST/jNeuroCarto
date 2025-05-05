package io.ast.jneurocarto.core;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class RandomElectrodeSelector<D extends ProbeDescription<T>, T> implements ElectrodeSelector<D, T> {

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
    public T select(D desp, T chmap, List<ElectrodeDescription> blueprint) {
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

        if (!ignorePreSelected) {
            for (var electrode : blueprint) {
                if (electrode.category() == ProbeDescription.CATE_SET) {
                    add(desp, ret, cand, electrode);
                }
            }
        }

        if (!ignoreExclude) {
            for (var electrode : blueprint) {
                if (electrode.category() == ProbeDescription.CATE_EXCLUDED) {
                    cand.remove(electrode);
                }
            }
        }

        return selectLoop(desp, ret, cand);
    }

    private T selectLoop(D desp, T chmap, Map<ElectrodeDescription, ElectrodeDescription> cand) {
        while (!cand.isEmpty()) {
            var e = pickElectrode(cand);
            if (e != null && !(e.category() == ProbeDescription.CATE_EXCLUDED && ignoreExclude)) {
                add(desp, chmap, cand, e);
            }
        }
        return chmap;
    }

    private @Nullable ElectrodeDescription pickElectrode(Map<ElectrodeDescription, ElectrodeDescription> cand) {
        if (cand.isEmpty()) return null;
        int pick = (int) (Math.random() * cand.size());
        return cand.keySet().stream().skip(pick).findFirst().orElse(null);
    }

    private void add(D desp, T chmap, Map<ElectrodeDescription, ElectrodeDescription> cand, ElectrodeDescription electrode) {
        var added = desp.addElectrode(chmap, electrode);
        if (added != null) {
            desp.getInvalidElectrodes(chmap, added, cand.keySet()).forEach(cand::remove);
        }
    }
}
