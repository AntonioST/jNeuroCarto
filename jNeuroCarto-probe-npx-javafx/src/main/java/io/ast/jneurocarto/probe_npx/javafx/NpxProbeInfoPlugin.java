package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.javafx.view.AbstractProbeInfoPlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;

@NullMarked
public class NpxProbeInfoPlugin extends AbstractProbeInfoPlugin<ChannelMap> {

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
        var bp = new Blueprint<>(new NpxProbeDescription(), chmap, blueprint);
        return switch (info) {
            case "used channels" -> getUsedChannels(bp);
            case "request electrodes" -> getRequestElectrodes(bp);
            case "channel efficiency" -> getChannelEfficiency(bp);
            default -> null;
        };
    }

    private String getUsedChannels(Blueprint<ChannelMap> blueprint) {
        var chmap = Objects.requireNonNull(blueprint.channelmap());
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

    private String getRequestElectrodes(Blueprint<ChannelMap> blueprint) {
        return "%.2f".formatted(ChannelMaps.requestElectrode(blueprint));
    }

    private String getChannelEfficiency(Blueprint<ChannelMap> blueprint) {
        var eff = ChannelMaps.channelEfficiency(blueprint);
        return "%.1f%%".formatted(100 * eff.efficiency());
    }
}
