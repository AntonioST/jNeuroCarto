package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.javafx.view.ProbeInfoPlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;

@NullMarked
public class NpxProbeInfoPlugin extends ProbeInfoPlugin<ChannelMap> {

    public NpxProbeInfoPlugin() {
    }

    @Override
    public List<String> listAllInfoLabels() {
        return List.of(
          "used channels",
          "request electrodes",
          "channel efficiency"
        );
    }

    @Override
    public @Nullable String getInfoValue(String info, ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        return switch (info) {
            case "used channels" -> getUsedChannels(chmap);
            case "request electrodes" -> getRequestElectrodes(blueprint);
            case "channel efficiency" -> getChannelEfficiency(chmap, blueprint);
            default -> null;
        };
    }

    private String getUsedChannels(ChannelMap chmap) {
        var used = chmap.size();
        var total = chmap.nChannel();
        var channels = chmap.channels();
        var ups = IntStream.range(0, chmap.nShank()).mapToObj(shank -> {
            var count = (int) channels.stream()
              .filter(Objects::nonNull)
              .filter(it -> it.shank == shank)
              .count();
            return "s%d=%d".formatted(shank, count);
        }).collect(Collectors.joining(", "));
        return "%d, total=%d (%s)".formatted(used, total, ups);
    }

    private String getRequestElectrodes(List<ElectrodeDescription> blueprint) {
        return "%.2f".formatted(ChannelMaps.requestElectrode(blueprint));
    }

    private String getChannelEfficiency(ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        var eff = ChannelMaps.channelEfficiency(chmap, blueprint);
        return "%.1f%%".formatted(100 * eff.efficiency());
    }
}
