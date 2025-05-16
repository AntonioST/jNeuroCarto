package io.ast.jneurocarto.javafx.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.javafx.utils.StylesheetsUtils;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;

@NullMarked
public class ProbeView<T> extends InteractionXYChart<ScatterChart<Number, Number>> {

    private final CartoConfig config;
    private final ProbeDescription<T> probe;
    private final Map<String, ScatterChart.Series<Number, Number>> electrodes = new HashMap<>();
    private final Map<Integer, String> allStateMap;
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

        allStateMap = probe.allStates();

        scatter.getData().add(newElectrodeSeries(STATE_HIGHLIGHTED));
        log.debug("add series {}", STATE_HIGHLIGHTED);

        var series = allStateMap.values().stream()
          .map(this::newElectrodeSeries)
          .toList();

        scatter.getData().addAll(series);
        if (log.isDebugEnabled()) {
            for (var s : series) {
                log.debug("add series {}", s.getName());
            }
        }
    }

    public static ScatterChart.Data<Number, Number> asData(ElectrodeDescription e) {
        return new ScatterChart.Data<>(e.x(), e.y(), e);
    }

//    public static Series<Number, Number> asSeries(String name, List<ElectrodeDescription> e) {
//        var ret = new Series<Number, Number>();
//        ret.setName(name);
//        ret.getData().addAll(e.stream().map(ProbeView::asData).toList());
//        return ret;
//    }

    private ScatterChart.Series<Number, Number> newElectrodeSeries(String name) {
        var series = new ScatterChart.Series<Number, Number>();
        series.setName(name);
        electrodes.put(name, series);
        return series;
    }

    /**
     * replace corresponding series by the electrodes at certain state.
     *
     * @param code state code
     * @param e
     * @return updated series.
     */
    private ScatterChart.Series<Number, Number> setSeries(int code, List<ElectrodeDescription> e) {
        var series = electrodes.get(probe.stateOf(code));
        if (series == null) throw new IllegalArgumentException();
        var c = e.stream().filter(it -> it.state() == code).toList();
        return setSeries(series, c);
    }

    /**
     * replace corresponding series by the electrodes.
     *
     * @param name series name.
     * @param e    electrodes.
     * @return updated series.
     */
    private ScatterChart.Series<Number, Number> setSeries(String name, List<ElectrodeDescription> e) {
        var series = electrodes.get(name);
        if (series == null) throw new IllegalArgumentException();
        return setSeries(series, e);
    }


    private ScatterChart.Series<Number, Number> setSeries(ScatterChart.Series<Number, Number> series, List<ElectrodeDescription> e) {
        series.getData().clear();
        if (!e.isEmpty()) {
            addSeries(series, e);
        }
        return series;
    }

    private ScatterChart.Series<Number, Number> addSeries(ScatterChart.Series<Number, Number> series, List<ElectrodeDescription> e) {
        series.getData().addAll(e.stream().map(ProbeView::asData).toList());
        var style = getCssStyleClass(series.getName());
        if (style != null) {
            applyStyleClass(series, style);
        }
        return series;
    }

    private @Nullable String getCssStyleClass(String state) {
        if (STATE_HIGHLIGHTED.equals(state)) {
            return "electrode-highlighted";
        }

        var code = probe.stateOf(state);
        if (code.isEmpty()) return null;

        return "electrode-state-%d".formatted(code.getAsInt());
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
        setBlueprint(probe.allElectrodes(channelmap));
    }

    /*===========*
     * blueprint *
     *===========*/

    public @Nullable List<ElectrodeDescription> getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(List<ElectrodeDescription> blueprint) {
        log.debug("setBlueprint");
        this.blueprint = blueprint;
        resetElectrodeState();
    }

    /**
     * reset blueprint, and updating {@link ElectrodeDescription#state()} by {@link #channelmap}'s channels.
     */
    public void resetElectrodeState() {
        var blueprint = this.blueprint;
        if (blueprint == null) return;

        log.debug("resetElectrodeState");
        for (var e : blueprint) {
            e.state(ProbeDescription.STATE_UNUSED);
        }

        var channelmap = this.channelmap;
        if (channelmap == null) return;

        var channels = probe.allChannels(channelmap, blueprint);
        for (var e : probe.getInvalidElectrodes(channelmap, channels, blueprint)) {
            e.state(ProbeDescription.STATE_DISABLED);
        }
        for (var e : channels) {
            e.state(ProbeDescription.STATE_USED);
        }
    }

    public void updateElectrodeState() {
        var channelmap = this.channelmap;
        if (channelmap == null) return;

        var blueprint = this.blueprint;
        if (blueprint == null) return;

        log.debug("updateElectrodeState");

        var channels = probe.allChannels(channelmap, blueprint);
        for (var e : probe.getInvalidElectrodes(channelmap, channels, blueprint)) {
            e.state(ProbeDescription.STATE_DISABLED);
        }
        for (var e : channels) {
            e.state(ProbeDescription.STATE_USED);
        }
    }

    public void updateElectrode() {
        var blueprint = this.blueprint;
        if (blueprint == null) return;

        log.debug("updateElectrode");
        for (var state : allStateMap.keySet()) {
            setSeries(state, blueprint);
        }

        setSeries(STATE_HIGHLIGHTED, List.of());
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

        updateElectrodeState();
    }

    /*============================*
     * capturing and highlighting *
     *============================*/

    public List<ElectrodeDescription> getCaptured(boolean reset) {
        var ret = new ArrayList<ElectrodeDescription>();
        for (var series : this.electrodes.values()) {
            ret.addAll(getCaptured(series, reset));
        }
        return ret;
    }

    public List<ElectrodeDescription> getCaptured(int state, boolean reset) {
        return getCaptured(allStateMap.get(state), reset);
    }

    public List<ElectrodeDescription> getCaptured(String name, boolean reset) {
        var series = electrodes.get(name);
        if (series == null) throw new IllegalArgumentException();
        return getCaptured(series, reset);
    }

    private List<ElectrodeDescription> getCaptured(XYChart.Series<Number, Number> series, boolean reset) {
        return series.getData().stream()
          .filter(it -> StylesheetsUtils.hasStyleClass(it.getNode(), CLASS_CAPTURED))
          .peek(it -> {
              if (reset) {
                  StylesheetsUtils.removeStyleClass(it.getNode(), CLASS_CAPTURED);
              }
          }).map(it -> (ElectrodeDescription) it.getExtraValue())
          .toList();
    }

    public void setCaptured(List<ElectrodeDescription> electrodes) {
        for (var series : this.electrodes.values()) {
            setCaptured(series, electrodes);
        }
    }

    private void setCaptured(XYChart.Series<Number, Number> series, List<ElectrodeDescription> electrodes) {
        series.getData().stream()
          .filter(it -> electrodes.contains((ElectrodeDescription) it.getExtraValue()))
          .forEach(it -> StylesheetsUtils.addStyleClass(it.getNode(), CLASS_CAPTURED));
    }

    public void clearCaptured() {
        for (var series : electrodes.values()) {
            getCaptured(series, true);
        }
    }

    public List<ElectrodeDescription> getHighlighted() {
        var ret = electrodes.get(STATE_HIGHLIGHTED).getData().stream()
          .map(it -> (ElectrodeDescription) it.getExtraValue())
          .toList();

        return probe.copyElectrodes(ret);
    }

    public void setHighlight(List<ElectrodeDescription> electrodes, boolean includeInvalid) {
        var chmap = this.channelmap;
        if (includeInvalid && chmap != null) {
            electrodes = probe.getInvalidElectrodes(chmap, electrodes, probe.allElectrodes(chmap));
        }

        var s = setSeries(STATE_HIGHLIGHTED, electrodes);
        var style = getCssStyleClass(STATE_HIGHLIGHTED);
        if (style != null) {
            applyStyleClass(s, style);
        }
    }

    public void clearHighlight() {
        setSeries(STATE_HIGHLIGHTED, List.of());
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

        for (var entry : electrodes.entrySet()) {
            entry.getValue().getData().stream()
              .filter(it -> {
                  var x = it.getXValue().doubleValue();
                  var y = it.getYValue().doubleValue();
                  var ret = e.bounds.contains(x, y);
                  if (ret) {
                      StylesheetsUtils.addStyleClass(it.getNode(), CLASS_CAPTURED);
                  } else {
                      StylesheetsUtils.removeStyleClass(it.getNode(), CLASS_CAPTURED);
                  }
                  return ret;
              })
              .map(it -> (ElectrodeDescription) it.getExtraValue())
              .forEach(captured::add);
        }

        setHighlight(captured, true);
    }

}
