package io.ast.jneurocarto.javafx.app;

import java.util.*;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.Blueprint;
import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.javafx.utils.StylesheetsUtils;
import javafx.geometry.Bounds;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;

@NullMarked
public class ProbeView<T> extends InteractionXYChart<ScatterChart<Number, Number>> {


    private final CartoConfig config;
    private final ProbeDescription<T> probe;
    private final Map<String, CodedSeries> electrodes = new HashMap<>();
    private static final String STATE_HIGHLIGHTED = "_highlighted_";
    private static final String CLASS_CAPTURED = "captured";

    private @Nullable T channelmap;
    private @Nullable List<ElectrodeDescription> blueprint;
    private final Logger log = LoggerFactory.getLogger(ProbeView.class);

    public ProbeView(CartoConfig config, ProbeDescription<T> probe) {
        var x = new NumberAxis("(um)", 0, 1000, 100);
        var y = new NumberAxis("(um)", 0, 1000, 100);
        var scatter = new ScatterChart<>(x, y);
        scatter.setAnimated(false);
        scatter.setLegendVisible(false);
        scatter.setVerticalZeroLineVisible(false);
        scatter.setHorizontalZeroLineVisible(false);

        super(scatter);
        log.debug("init");

        this.config = config;
        this.probe = probe;

        setOnDataTouch(this::onElectrodeTouch);
        setOnDataSelect(this::onElectrodeSelect);

        getStylesheets().add(getClass().getResource("/style-sheet/probe-view.css").toExternalForm());

        var empty = List.<ElectrodeDescription>of();

        var series = probe.allStates().values().stream()
          .map(state -> newSeries(state, empty))
          .toList();

        scatter.getData().add(newSeries(STATE_HIGHLIGHTED, empty));
        scatter.getData().addAll(series);
    }

    private ScatterChart.Series<Number, Number> newSeries(String name, List<ElectrodeDescription> electrodes) {
        var code = probe.stateOf(name).orElse(-1);

        var series = new ScatterChart.Series<Number, Number>();
        series.setName(name);
        this.electrodes.put(name, new CodedSeries(code, series));

        var s = electrodes.stream()
          .map(it -> new ScatterChart.Data<Number, Number>(it.x(), it.y(), it))
          .toList();

        series.getData().addAll(s);

        return series;
    }

    private void resetSeries(List<ElectrodeDescription> electrodes) {
        var states = new ArrayList<>(this.electrodes.keySet());

        // maintain z-order
        states.sort(Comparator.comparingInt(it -> probe.stateOf(it).orElse(-1)));

        var items = new ArrayList<ScatterChart.Series<Number, Number>>(this.electrodes.size());
        for (var state : states) {
            items.add(newSeries(state, electrodes));
        }
        var data = getChart().getData();
        data.clear();
        data.addAll(items);

        for (var series : this.electrodes.values()) {
            series.setVisible(false);
            var style = getCssStyleClass(series.name());
            if (style != null) series.applyStyleClass(style);
        }
    }

    private @Nullable String getCssStyleClass(String state) {
        if (STATE_HIGHLIGHTED.equals(state)) {
            return "electrode-highlighted";
        }

        var code = probe.stateOf(state);
        if (code.isEmpty()) return null;

        return "electrode-state-%d".formatted(code.getAsInt());
    }

    record CodedSeries(int code, ScatterChart.Series<Number, Number> series) {
        String name() {
            return series.getName();
        }

        private void applyStyleClass(String style) {
            series.getData().forEach(it -> InteractionXYChart.applyStyleClass(it, style));
        }

        private List<ElectrodeDescription> getVisible() {
            return series.getData().stream()
              .filter(it -> it.getNode().isVisible())
              .map(it -> (ElectrodeDescription) it.getExtraValue())
              .toList();
        }

        private void setVisible(boolean visible) {
            series.getData().forEach(it -> it.getNode().setVisible(visible));
        }

        private void setVisible(Set<ElectrodeDescription> electrodes) {
            series.getData().forEach(it -> {
                var visible = electrodes.contains((ElectrodeDescription) it.getExtraValue());
                it.getNode().setVisible(visible);
            });
        }

        private List<ElectrodeDescription> getCaptured(boolean reset) {
            return series.getData().stream()
              .filter(it -> StylesheetsUtils.hasStyleClass(it.getNode(), CLASS_CAPTURED))
              .peek(it -> {
                  if (reset) {
                      StylesheetsUtils.removeStyleClass(it.getNode(), CLASS_CAPTURED);
                  }
              }).map(it -> (ElectrodeDescription) it.getExtraValue())
              .toList();
        }

        private List<ElectrodeDescription> getCaptured(Bounds bounds, boolean set) {
            return series.getData().stream().filter(it -> {
                  var x = it.getXValue().doubleValue();
                  var y = it.getYValue().doubleValue();
                  var ret = it.getNode().isVisible() && bounds.contains(x, y);
                  if (set) {
                      if (ret) {
                          StylesheetsUtils.addStyleClass(it.getNode(), CLASS_CAPTURED);
                      } else {
                          StylesheetsUtils.removeStyleClass(it.getNode(), CLASS_CAPTURED);
                      }
                  }
                  return ret;
              }).map(it -> (ElectrodeDescription) it.getExtraValue())
              .toList();
        }

        private void setCapture(boolean captured) {
            if (captured) {
                series.getData().forEach(it -> StylesheetsUtils.addStyleClass(it.getNode(), CLASS_CAPTURED));
            } else {
                series.getData().forEach(it -> StylesheetsUtils.removeStyleClass(it.getNode(), CLASS_CAPTURED));
            }
        }

        private void setCapture(Set<ElectrodeDescription> electrodes) {
            for (var data : series.getData()) {
                var captured = electrodes.contains((ElectrodeDescription) data.getExtraValue());
                if (captured) {
                    StylesheetsUtils.addStyleClass(data.getNode(), CLASS_CAPTURED);
                } else {
                    StylesheetsUtils.removeStyleClass(data.getNode(), CLASS_CAPTURED);
                }
            }
        }
    }

    /*============*
     * channelmap *
     *============*/

    public @Nullable T getChannelmap() {
        return channelmap;
    }

    /**
     * @return new channelmap, {@code null} if failed.
     */
    public @Nullable T resetChannelmap() {
        var channelmap = this.channelmap;
        if (channelmap == null) {
            blueprint = null;
            return null;
        }

        var code = probe.channelmapCode(channelmap);
        if (code == null) return null;

        return resetChannelmap(code);
    }

    public T resetChannelmap(String code) {
        log.debug("resetChannelmap(code={})", code);
        var channelmap = probe.newChannelmap(code);
        setChannelmap(channelmap);
        return channelmap;
    }

    public void setChannelmap(T channelmap) {
        log.debug("resetChannelmap({})", channelmap);
        this.channelmap = channelmap;

        var allElectrode = probe.allElectrodes(channelmap);
        resetSeries(allElectrode);
    }

    /*===========*
     * blueprint *
     *===========*/

    /**
     * @return unmodifiable blueprint
     */
    public @Nullable List<ElectrodeDescription> getBlueprint() {
        var channelmap = this.channelmap;
        if (channelmap == null) return null;

        if (this.blueprint == null) {
            resetBlueprint();
        }

        return Collections.unmodifiableList(Objects.requireNonNull(blueprint));
    }

    public void setBlueprint(List<ElectrodeDescription> blueprint) {
        var channelmap = this.channelmap;
        if (channelmap == null) return;

        log.debug("setBlueprint");
        this.blueprint = new Blueprint<>(probe, channelmap)
          .setBlueprint(blueprint)
          .applyBlueprint()
          .electrodes();

        resetElectrodeState();
    }

    public @Nullable List<ElectrodeDescription> resetBlueprint() {
        var channelmap = this.channelmap;
        if (channelmap == null) return null;

        log.debug("resetBlueprint");
        setBlueprint(probe.allElectrodes(channelmap));
        return Objects.requireNonNull(blueprint);
    }

    /**
     * reset blueprint, and updating {@link ElectrodeDescription#state()} by {@link #channelmap}'s channels.
     */
    public void resetElectrodeState() {
        var blueprint = this.blueprint;
        if (blueprint == null) return;

        log.debug("resetElectrodeState");

        var channelmap = this.channelmap;
        List<ElectrodeDescription> channels;
        if (channelmap == null) {
            channels = List.of();
        } else {
            channels = probe.allChannels(channelmap, blueprint);
        }

        for (var e : blueprint) {
            e.state(ProbeDescription.STATE_UNUSED);
        }
        if (channelmap != null) {
            for (var e : probe.getInvalidElectrodes(channelmap, channels, blueprint)) {
                e.state(ProbeDescription.STATE_DISABLED);
            }
        }
        for (var e : channels) {
            e.state(ProbeDescription.STATE_USED);
        }
    }

    public void updateElectrode() {
        var blueprint = this.blueprint;
        if (blueprint == null) return;

        log.debug("updateElectrode");
        for (var e : electrodes.entrySet()) {
            var series = e.getValue();
            var state = series.code;
            if (state >= 0) {
                var set = blueprint.stream()
                  .filter(it -> it.state() == state)
                  .collect(Collectors.toSet());
                series.setVisible(set);
            }
        }

        electrodes.get(STATE_HIGHLIGHTED).setVisible(false);
    }

    public void setStateForCaptured(int state) {
        var chmap = this.channelmap;
        if (chmap == null) return;

        log.debug("setStateForCaptured({})", probe.stateOf(state));
        List<ElectrodeDescription> captured;
        if (state == ProbeDescription.STATE_USED) {
            captured = getCaptured(ProbeDescription.STATE_UNUSED, true);
            log.debug("add {} electrodes", captured.size());
            for (var e : captured) {
                probe.addElectrode(chmap, e);
            }

            captured = getCaptured(ProbeDescription.STATE_DISABLED, true);
            log.debug("add {} electrodes forced", captured.size());
            for (var e : captured) {
                probe.addElectrode(chmap, e, true);
            }
        } else if (state == ProbeDescription.STATE_UNUSED) {
            captured = getCaptured(ProbeDescription.STATE_USED, true);
            log.debug("remove {} electrodes", captured.size());
            for (var e : captured) {
                probe.removeElectrode(chmap, e);
            }
        }

        resetElectrodeState();
    }

    public void setCategoryForCaptured(int category) {
        var chmap = this.channelmap;
        if (chmap == null) return;

        log.debug("setCategoryForCaptured({})", probe.categoryOf(category));

        for (var e : getCaptured(true)) {
            e.category(category);
        }

        updateElectrode();
    }

    /*============================*
     * capturing and highlighting *
     *============================*/

    public List<ElectrodeDescription> getCaptured(boolean reset) {
        var ret = new ArrayList<ElectrodeDescription>();
        for (var series : this.electrodes.values()) {
            ret.addAll(series.getCaptured(reset));
        }
        return ret;
    }

    public List<ElectrodeDescription> getCaptured(int state, boolean reset) {
        var name = probe.stateOf(state);
        if (name == null) throw new IllegalArgumentException();
        return getCaptured(name, reset);
    }

    public List<ElectrodeDescription> getCaptured(String name, boolean reset) {
        var series = electrodes.get(name);
        if (series == null) throw new IllegalArgumentException();
        return series.getCaptured(reset);
    }

    public void setCaptured(List<ElectrodeDescription> electrodes) {
        var s = new HashSet<>(electrodes);
        for (var series : this.electrodes.values()) {
            series.setCapture(s);
        }
    }

    public void clearCaptured() {
        for (var series : electrodes.values()) {
            series.setCapture(false);
        }
    }

    public List<ElectrodeDescription> getHighlighted() {
        var ret = electrodes.get(STATE_HIGHLIGHTED).getVisible();
        return probe.copyElectrodes(ret);
    }

    public void setHighlight(List<ElectrodeDescription> electrodes, boolean includeInvalid) {
        var chmap = this.channelmap;
        log.debug("setHighlight {} electrodes", electrodes.size());
        if (includeInvalid && chmap != null) {
            electrodes = probe.getInvalidElectrodes(chmap, electrodes, probe.allElectrodes(chmap));
            log.debug("setHighlight {} invalid electrodes", electrodes.size());
        }

        this.electrodes.get(STATE_HIGHLIGHTED).setVisible(new HashSet<>(electrodes));
    }

    public void clearHighlight() {
        electrodes.get(STATE_HIGHLIGHTED).setVisible(false);
    }

    /*====*
     * UI *
     *====*/

    public void fitAxesBoundaries() {
        var blueprint = this.blueprint;
        if (blueprint == null) {
            resetAxesBoundaries();
            return;
        }

        var x1 = 0.0;
        var x2 = 0.0;
        var y1 = 0.0;
        var y2 = 0.0;
        for (var e : blueprint) {
            var x = e.x();
            var y = e.y();
            x1 = Math.min(x1, x);
            x2 = Math.max(x2, x);
            y1 = Math.min(y1, y);
            y2 = Math.max(y2, y);
        }
        var dx = (x2 - x1) / 50;
        var dy = (y2 - y1) / 100;

        setAxesBoundaries(x1 - dx, x2 + dx, y1 - dy, y2 + dy);
    }

    /*==============*
     * event handle *
     *==============*/

    private void onElectrodeTouch(DataTouchEvent e) {
    }

    private void onElectrodeSelect(DataSelectEvent e) {
        var captured = new ArrayList<ElectrodeDescription>();

        for (var series : electrodes.values()) {
            if (!series.name().equals(STATE_HIGHLIGHTED)) {
                captured.addAll(series.getCaptured(e.bounds, true));
            }
        }

        if (captured.isEmpty()) {
            clearHighlight();
        } else {
            setHighlight(captured, true);
        }
    }

}
