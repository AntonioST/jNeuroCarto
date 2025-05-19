package io.ast.jneurocarto.javafx.app;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.geometry.Bounds;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.Blueprint;
import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public class ProbeView<T> extends InteractionXYChart<ScatterChart<Number, Number>> {


    private static final String STATE_HIGHLIGHTED = "_highlighted_";
    private static final String CLASS_CAPTURED = "captured";

    private final CartoConfig config;
    private final ProbeDescription<T> probe;
    private final Map<String, CodedSeries> electrodes = new HashMap<>();
    private CodedSeries highlighted;

    private @Nullable T channelmap;
    private @Nullable List<ElectrodeDescription> blueprint;
    private final Logger log = LoggerFactory.getLogger(ProbeView.class);

    public ProbeView(CartoConfig config, ProbeDescription<T> probe) {
        var x = new NumberAxis("(um)", 0, 1000, 100);
        var y = new NumberAxis("(um)", 0, 1000, 100);
        var scatter = new ScatterChart<>(x, y);
        scatter.setAnimated(false);
        scatter.setLegendVisible(false);
        scatter.setVerticalZeroLineVisible(true);
        scatter.setHorizontalZeroLineVisible(true);

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
          .peek(it -> electrodes.put(it.name(), it))
          .map(CodedSeries::series)
          .toList();

        highlighted = newSeries(STATE_HIGHLIGHTED, empty);
        scatter.getData().add(highlighted.series);
        scatter.getData().addAll(series);
    }

    private CodedSeries newSeries(String name, List<ElectrodeDescription> electrodes) {
        var code = probe.stateOf(name).orElse(-1);

        var series = new ScatterChart.Series<Number, Number>();
        series.setName(name);

        var ret = new CodedSeries(code, series);
        setSeries(ret, electrodes);
        return ret;
    }

    private void setSeries(CodedSeries series, List<ElectrodeDescription> electrodes) {
        var s = electrodes.stream()
          .map(it -> new ScatterChart.Data<Number, Number>(it.x(), it.y(), it))
          .toList();

        var data = series.series.getData();
        data.clear();
        data.addAll(s);
    }

    private void resetSeries(List<ElectrodeDescription> electrodes) {
        var scatter = getChart();

        var ret = scatter.getData().stream().map(series -> {
              var name = series.getName();
              var newSeries = newSeries(name, electrodes);
              if (STATE_HIGHLIGHTED.equals(name)) {
                  highlighted = newSeries;
              } else {
                  this.electrodes.put(name, newSeries);
              }
              return newSeries;
          }).map(CodedSeries::series)
          .toList();

        scatter.getData().clear();
        scatter.getData().addAll(ret);

        this.electrodes.values().forEach(it -> {
            it.setVisible(false);
            getCssStyleClass(it.name()).ifPresent(it::applyStyleClass);
        });
        highlighted.setVisible(false);
        getCssStyleClass(STATE_HIGHLIGHTED).ifPresent(highlighted::applyStyleClass);
    }

    private Optional<String> getCssStyleClass(String state) {
        if (STATE_HIGHLIGHTED.equals(state)) {
            return Optional.of("electrode-highlighted");
        }

        var code = probe.stateOf(state);
        if (code.isEmpty()) return Optional.empty();

        var cls = "electrode-state-%d".formatted(code.getAsInt());
        return Optional.of(cls);
    }

    record CodedSeries(int code, ScatterChart.Series<Number, Number> series) {
        String name() {
            return series.getName();
        }

        private void applyStyleClass(String style) {
            InteractionXYChart.applyStyleClass(series, style);
        }

        private List<ElectrodeDescription> getVisible() {
            return toElectrodeList(InteractionXYChart.getVisible(series));
        }

        private void setVisible(boolean visible) {
            InteractionXYChart.setVisible(series, visible);
        }

        private void setVisible(Set<ElectrodeDescription> electrodes) {
            InteractionXYChart.setVisible(series, ElectrodeDescription.class, electrodes::contains);
        }

        private List<ElectrodeDescription> getCaptured(boolean reset) {
            Stream<XYChart.Data<Number, Number>> stream;
            if (reset) {
                stream = InteractionXYChart.filterAndRemoveStyleClass(series, CLASS_CAPTURED);
            } else {
                stream = InteractionXYChart.filterStyleClass(series, CLASS_CAPTURED);
            }

            return toElectrodeList(stream);
        }

        private List<ElectrodeDescription> getCaptured(Bounds bounds, boolean set) {
            Stream<XYChart.Data<Number, Number>> stream;
            if (set) {
                stream = InteractionXYChart.filterInBoundAndSetStyleClass(series, bounds, CLASS_CAPTURED, false);
            } else {
                stream = InteractionXYChart.filterInBound(series, bounds, false);
            }

            return toElectrodeList(stream);
        }

        private void setCapture(boolean captured) {
            if (captured) {
                InteractionXYChart.applyStyleClass(series, CLASS_CAPTURED);
            } else {
                InteractionXYChart.removeStyleClass(series, CLASS_CAPTURED);
            }
        }

        private void setCapture(Set<ElectrodeDescription> electrodes) {
            InteractionXYChart.filterExtraValueAndSetStyleClass(series, CLASS_CAPTURED, ElectrodeDescription.class, electrodes::contains)
              .forEach((Consumer<? super XYChart.Data<Number, Number>>) _ -> {
              });
        }

        private static List<ElectrodeDescription> toElectrodeList(Stream<XYChart.Data<Number, Number>> stream) {
            return stream.map(it -> (ElectrodeDescription) it.getExtraValue())
              .toList();
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

    public void clearChannelmap() {
        log.debug("clearChannelmap");
        channelmap = null;
        blueprint = null;

        resetSeries(List.of());
        updateElectrode();
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

        if (blueprint != null) {
            log.debug("updateElectrode");
            for (var series : electrodes.values()) {
                var state = series.code;
                var set = blueprint.stream()
                  .filter(it -> it.state() == state)
                  .collect(Collectors.toSet());
                series.setVisible(set);
            }

            highlighted.setVisible(false);
        }

        var scatter = getChart();
        scatter.setVerticalZeroLineVisible(blueprint == null);
        scatter.setHorizontalZeroLineVisible(blueprint == null);
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

            clearCaptured();
        } else if (state == ProbeDescription.STATE_UNUSED) {
            captured = getCaptured(ProbeDescription.STATE_USED, true);
            log.debug("remove {} electrodes", captured.size());
            for (var e : captured) {
                probe.removeElectrode(chmap, e);
            }

            clearCaptured();
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
        return probe.copyElectrodes(highlighted.getVisible());
    }

    public void setHighlight(List<ElectrodeDescription> electrodes, boolean includeInvalid) {
        var chmap = this.channelmap;
        log.trace("setHighlight {} electrodes", electrodes.size());
        highlighted.setCapture(new HashSet<>(electrodes));

        if (includeInvalid && chmap != null) {
            electrodes = probe.getInvalidElectrodes(chmap, electrodes, probe.allElectrodes(chmap));
            log.trace("setHighlight {} invalid electrodes", electrodes.size());
        }

        highlighted.setVisible(new HashSet<>(electrodes));
    }

    public void clearHighlight() {
        highlighted.setCapture(false);
        highlighted.setVisible(false);
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
            var electrodes = series.getCaptured(e.bounds, true);
            log.trace("captured {} {} electrodes", electrodes.size(), series.name());
            captured.addAll(electrodes);
        }

        if (captured.isEmpty()) {
            clearHighlight();
        } else {
            setHighlight(captured, true);
        }
    }

}
