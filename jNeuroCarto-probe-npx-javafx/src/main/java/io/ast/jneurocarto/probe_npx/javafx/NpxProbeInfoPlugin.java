package io.ast.jneurocarto.probe_npx.javafx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.view.InvisibleView;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@NullMarked
public class NpxProbeInfoPlugin extends InvisibleView implements ProbePlugin<ChannelMap> {

    public NpxProbeInfoPlugin() {
    }

    @Override
    public String name() {
        return "Probe information";
    }

    /*===========*
     * UI Layout *
     *===========*/

    private @Nullable List<Label> labels;
    private @Nullable List<Label> contents;

    @Override
    protected Node setupContent(PluginSetupService service) {
        var font = Font.font("FreeMono");
        var labels = listAllInfoLabels();
        var size = labels.size();
        this.labels = new ArrayList<>(size);
        contents = new ArrayList<>(size);

        var layout = new GridPane();
        for (int i = 0; i < size; i++) {
            var label = new Label(labels.get(i));
            label.setFont(font);
            this.labels.add(label);
            layout.add(label, 0, i);

            var content = new Label("");
            content.setFont(font);
            contents.add(content);
            layout.add(content, 1, i);
        }

        var c1 = new ColumnConstraints(120);
        var c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);

        layout.getColumnConstraints().addAll(c1, c2);

        return layout;
    }

    @Override
    public void onProbeUpdate(ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        var labels = this.labels;
        var contents = this.contents;
        if (labels == null || contents == null) return;

        var log = LoggerFactory.getLogger(getClass());

        for (int i = 0, size = labels.size(); i < size; i++) {
            var label = labels.get(i).getText();
            var content = contents.get(i);
            String value;

            try {
                value = getInfoValue(label, chmap, blueprint);
            } catch (Exception e) {
                value = null;
                log.warn(label, e);
            }

            content.setText(value == null ? "" : value);
        }
    }

    /*==============*
     * information  *
     *==============*/

    public List<String> listAllInfoLabels() {
        return List.of(
          "used channels",
          "request electrodes",
          "channel efficiency"
        );
    }

    public @Nullable String getInfoValue(String info, ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        var bp = new Blueprint<>(new NpxProbeDescription(), chmap, blueprint);
        if (chmap.type() instanceof NpxProbeType.NP2020) {
            return getInfoValueNp2020(info, bp);
        } else {
            return getInfoValueNp0(info, bp);
        }
    }

    private @Nullable String getInfoValueNp0(String info, Blueprint<ChannelMap> blueprint) {
        return switch (info) {
            case "used channels" -> getUsedChannels(blueprint);
            case "request electrodes" -> getRequestElectrodes(blueprint);
            case "channel efficiency" -> getChannelEfficiency(blueprint);
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

    /*================*
     * np2020 support *
     *================*/

    private @Nullable String getInfoValueNp2020(String info, Blueprint<ChannelMap> blueprint) {
        return switch (info) {
            case "used channels" -> getUsedChannelsNp2020(blueprint);
            case "request electrodes" -> getRequestElectrodesNp2020(blueprint);
            case "channel efficiency" -> getChannelEfficiencyNp2020(blueprint);
            default -> null;
        };
    }

    private String getUsedChannelsNp2020(Blueprint<ChannelMap> blueprint) {
        var chmap = Objects.requireNonNull(blueprint.channelmap());

        var used = chmap.size();
        var total = chmap.nChannel();
        var channels = chmap.channels();
        var ups = IntStream.range(0, chmap.nShank()).mapToObj(shank -> {
            var count = (int) channels.stream()
              .filter(Objects::nonNull)
              .filter(it -> it.shank == shank)
              .count();
            return "s%d=%-5d".formatted(shank, count);
        }).collect(Collectors.joining(", "));
        return "%s, total=%d/%d".formatted(ups, used, total);
    }

    private String getRequestElectrodesNp2020(Blueprint<ChannelMap> blueprint) {
        assert NpxProbeType.np2020.nShank() == 4;
        var toolkit = new BlueprintToolkit<>(blueprint);
        var s0 = toolkit.mask(e -> e.s() == 0);
        var s1 = toolkit.mask(e -> e.s() == 1);
        var s2 = toolkit.mask(e -> e.s() == 2);
        var s3 = toolkit.mask(e -> e.s() == 3);
        var r0 = ChannelMaps.requestElectrode(toolkit, s0);
        var r1 = ChannelMaps.requestElectrode(toolkit, s1);
        var r2 = ChannelMaps.requestElectrode(toolkit, s2);
        var r3 = ChannelMaps.requestElectrode(toolkit, s3);
        var rr = (r0 + r1 + r2 + r3) / 4;

        return "s0=%5.1f, s1=%5.1f, s2=%5.1f, s3=%5.1f, mean:%.2f".formatted(r0, r1, r2, r3, rr);
    }

    private String getChannelEfficiencyNp2020(Blueprint<ChannelMap> blueprint) {
        assert NpxProbeType.np2020.nShank() == 4;
        var toolkit = new BlueprintToolkit<>(blueprint);
        var s0 = toolkit.mask(e -> e.s() == 0);
        var s1 = toolkit.mask(e -> e.s() == 1);
        var s2 = toolkit.mask(e -> e.s() == 2);
        var s3 = toolkit.mask(e -> e.s() == 3);
        var r0 = ChannelMaps.channelEfficiency(toolkit, s0).efficiency() * 100;
        var r1 = ChannelMaps.channelEfficiency(toolkit, s1).efficiency() * 100;
        var r2 = ChannelMaps.channelEfficiency(toolkit, s2).efficiency() * 100;
        var r3 = ChannelMaps.channelEfficiency(toolkit, s3).efficiency() * 100;
        var rr = (r0 + r1 + r2 + r3) / 4;

        return "s0=%5.1f, s1=%5.1f, s2=%5.1f, s3=%5.1f, mean:%.2f%%".formatted(r0, r1, r2, r3, rr);
    }
}
