package io.ast.jneurocarto.probe_npx.javafx;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.javafx.app.PluginSetupService;
import io.ast.jneurocarto.javafx.view.ProbePlugin;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import io.ast.jneurocarto.probe_npx.ReferenceInfo;

@NullMarked
public class ProbeReferencePlugin implements ProbePlugin<ChannelMap> {

    private final NpxProbeDescription probe;
    private @Nullable ChannelMap chmap;
    private final Logger log = LoggerFactory.getLogger(ProbeReferencePlugin.class);

    public ProbeReferencePlugin(NpxProbeDescription probe) {
        this.probe = probe;
        reference.addListener((_, _, info) -> onReferenceChanged(info));
    }

    @Override
    public String description() {
        return "set Neuropixels reference";
    }

    @Override
    public void onProbeUpdate(ChannelMap chmap, List<ElectrodeDescription> blueprint) {
        var needUpdate = this.chmap == null;
        var newCode = Objects.requireNonNull(probe.channelmapCode(chmap));
        if (this.chmap != null) {
            var oldCode = Objects.requireNonNull(probe.channelmapCode(this.chmap));
            if (!Objects.equals(oldCode, newCode)) {
                needUpdate = true;
            }
        }

        this.chmap = chmap;

        if (needUpdate) updateReferenceChoices();
        setReferenceInfo(chmap.getReferenceInfo());
    }

    /*============*
     * properties *
     *============*/

    public final ObjectProperty<@Nullable ReferenceInfo> reference = new SimpleObjectProperty<>();

    public @Nullable ReferenceInfo getReferenceInfo() {
        return reference.get();
    }

    public void setReferenceInfo(ReferenceInfo info) {
        reference.set(info);
    }

    /*===========*
     * UI layout *
     *===========*/

    private ChoiceBox<ReferenceInfo> references;

    @Override
    public @Nullable Node setup(PluginSetupService service) {
        log.debug("setup");

        references = new ChoiceBox<>();
        references.setConverter(new RefIntoToString());
        references.setDisable(true);

        var root = new HBox(
          new Label("Reference"),
          references
        );
        root.setSpacing(5);

        return root;
    }

    private void onReferenceChanged(@Nullable ReferenceInfo info) {
        var chmap = this.chmap;
        if (info != null) {
            if (chmap != null) {
                chmap.setReference(info.code());
            }
            references.setValue(info);
        }
    }

    private void updateReferenceChoices() {
        var chmap = this.chmap;
        if (chmap == null) {
            references.getItems().clear();
            references.setDisable(true);
            return;
        }

        var type = chmap.type();
        var infos = IntStream.range(0, ReferenceInfo.maxReferenceValue(type))
          .mapToObj(code -> ReferenceInfo.of(type, code))
          .toList();

        references.getItems().clear();
        references.getItems().addAll(infos);
        references.setDisable(false);
    }

    private class RefIntoToString extends StringConverter<ReferenceInfo> {
        @Override
        public String toString(@Nullable ReferenceInfo object) {
            if (object == null) return "";
            return switch (object.type()) {
                case ReferenceInfo.ReferenceType.EXT -> "External";
                case ReferenceInfo.ReferenceType.TIP -> "Shank: " + object.shank();
                case ReferenceInfo.ReferenceType.ON_SHANK -> "Channel: " + object.shank() + ":" + object.channel();
            };
        }

        @Override
        public ReferenceInfo fromString(String string) {
            throw new UnsupportedOperationException();
        }
    }
}
