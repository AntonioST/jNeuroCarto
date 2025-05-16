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
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;

@NullMarked
public class ProbeView<T> extends InteractionXYChart<ScatterChart<Number, Number>> {

    private final CartoConfig config;
    private final ProbeDescription<T> probe;
    private final Map<String, ScatterChart.Series<Number, Number>> electrodes = new HashMap<>();
    private final Map<Integer, String> allStateMap;
    private static final String STATE_HIGHLIGHTED = "_highlighted_";

    private @Nullable T channelmap;
    private @Nullable List<ElectrodeDescription> blueprint;
    private final Logger log = LoggerFactory.getLogger(ProbeView.class);

    public ProbeView(CartoConfig config, ProbeDescription<T> probe) {
        var x = new NumberAxis("(um)", 0, 1000, 100);
        var y = new NumberAxis("(um)", 0, 1000, 100);
        var scatter = new ScatterChart<>(x, y);
        scatter.setLegendVisible(false);
        scatter.setVerticalZeroLineVisible(false);
        scatter.setHorizontalZeroLineVisible(false);

        super(scatter);
        log.debug("init");

        this.config = config;
        this.probe = probe;

        setOnDataTouch(this::onElectrodeTouch);
        setOnDataSelect(this::onElectrodeSelect);

        allStateMap = probe.allStates();

        var series = allStateMap.values().stream()
          .map(this::newElectrodeSeries)
          .toList();

        scatter.getData().addAll(series);
        if (log.isDebugEnabled()) {
            for (var s : series) {
                log.debug("add series {}", s.getName());
            }
        }

        scatter.getData().add(newElectrodeSeries(STATE_HIGHLIGHTED));
        log.debug("add series {}", STATE_HIGHLIGHTED);
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
        var style = getCssStyleByState(series.getName());
        if (style != null) {
            applyStyle(series, style);
        }
        return series;
    }

    private @Nullable String getCssStyleByState(String state) {
        if (STATE_HIGHLIGHTED.equals(state)) {
            return """
              -fx-background-color: rgba(255,255,0,0.5);
              -fx-padding: 4px;
              -fx-background-radius: 4px;
              """;
        }

        var code = probe.stateOf(state);
        if (code.isEmpty()) return null;

        return switch (code.getAsInt()) {
            case ProbeDescription.STATE_UNUSED -> """
              -fx-background-color: black;
              -fx-padding: 2px;
              -fx-background-radius: 2px;
              """;
            case ProbeDescription.STATE_USED -> """
              -fx-background-color: green;
              -fx-padding: 2px;
              -fx-background-radius: 2px;
              """;
            case ProbeDescription.STATE_DISABLED -> """
              -fx-background-color: rgba(255,0,0,0.2);
              -fx-padding: 1px;
              -fx-background-radius: 1px;
              """;
            default -> null;
        };
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

        fitAxesBoundaries();
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

    /*==============*
     * highlighting *
     *==============*/

    public List<ElectrodeDescription> getHighlighted() {
        var ret = electrodes.get(STATE_HIGHLIGHTED).getData().stream()
          .map(it -> (ElectrodeDescription) it.getExtraValue())
          .toList();

        return probe.copyElectrodes(ret);
    }

    public void setHighlight(List<ElectrodeDescription> electrodes) {
        var s = setSeries(STATE_HIGHLIGHTED, electrodes);
        var style = getCssStyleByState(STATE_HIGHLIGHTED);
        if (style != null) {
            applyStyle(s, style);
        }
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
              .filter(it -> e.bounds.contains(it.getXValue().doubleValue(), it.getYValue().doubleValue()))
              .map(it -> (ElectrodeDescription) it.getExtraValue())
              .forEach(captured::add);
        }

        setHighlight(captured);
    }

}
