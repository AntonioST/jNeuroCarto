package io.ast.jneurocarto.javafx.app;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.cli.CartoConfig;
import io.ast.jneurocarto.javafx.chart.*;
import io.ast.jneurocarto.javafx.view.StateView;

@NullMarked
public class ProbeView<T> extends InteractionXYChart {

    private static final String STATE_HIGHLIGHTED = "_highlighted_";
    private static final Color COLOR_UNUSED = Color.BLACK;
    private static final Color COLOR_USED = Color.GREEN;
    private static final Color COLOR_DISABLE = Color.RED;
    private static final Color COLOR_HIGHLIGHTED = Color.ORANGE;

    private final CartoConfig config;
    private final ProbeDescription<T> probe;
    private final InteractionXYPainter interaction;
    private final Map<String, XYMarker> electrodes = new HashMap<>();
    private final Map<String, XYMarker> captured = new HashMap<>();
    private XYMarker highlighted;

    private @Nullable T channelmap;
    private @Nullable List<ElectrodeDescription> blueprint;
    private final Logger log = LoggerFactory.getLogger(ProbeView.class);

    public ProbeView(CartoConfig config, ProbeDescription<T> probe) {
        super();
        var x = getXAxis();
        var y = getYAxis();
        var scatter = getChart();
        x.setLabel("(um)");
        y.setLabel("(um)");
        scatter.setAnimated(false);
        scatter.setLegendVisible(false);
        scatter.setVerticalZeroLineVisible(true);
        scatter.setHorizontalZeroLineVisible(true);

        log.debug("init");

        this.config = config;
        this.probe = probe;
        interaction = getPlotting();

        setOnDataTouch(this::onElectrodeTouch);
        setOnDataSelect(this::onElectrodeSelect);

//        getStylesheets().add(getClass().getResource("/style-sheet/probe-view.css").toExternalForm());

        for (var name : probe.allStates().values()) {
            newMarkerData(name, electrodes);
        }
        for (var name : probe.allStates().values()) {
            var m = newMarkerData(name, captured);
            m.edgewidth(2);
            m.edge(m.fill());
        }

        highlighted = newMarkerData(STATE_HIGHLIGHTED, null);
    }

    private XYMarker newMarkerData(String name, @Nullable Map<String, XYMarker> collect) {
        var ret = new XYMarker();

        var code = probe.stateOf(name).orElse(-1);
        var color = switch (code) {
            case ProbeDescription.STATE_UNUSED -> COLOR_UNUSED;
            case ProbeDescription.STATE_USED -> COLOR_USED;
            case ProbeDescription.STATE_DISABLED -> COLOR_DISABLE;
            default -> null;
        };

        if (color != null) {
            ret.fill(color);
            if (code == ProbeDescription.STATE_DISABLED) {
                ret.alpha(0.2);
                ret.w(2);
                ret.h(2);
                ret.z(7);
            } else {
                ret.w(4);
                ret.h(4);
                ret.z(10);
            }
        } else if (STATE_HIGHLIGHTED.equals(name)) {
            ret.fill(COLOR_HIGHLIGHTED);
            ret.w(8);
            ret.h(6);
            ret.edgewidth(4);
            ret.z(5);
        }

        interaction.addGraphics(ret);
        if (collect != null) collect.put(name, ret);

        return ret;
    }

    private void setSeries(XYSeries series, Stream<ElectrodeDescription> electrodes) {
        series.clearData();
        series.addData(electrodes.map(it -> new XY(it.x(), it.y(), it)));
    }


    private static List<ElectrodeDescription> transferData(XYSeries src,
                                                           @Nullable XYSeries dst) {
        var ret = src.data().map(it -> (ElectrodeDescription) it.external()).toList();
        if (dst != null) {
            src.transferData(dst);
        }
        return ret;
    }

    private static List<ElectrodeDescription> transferData(XYSeries src,
                                                           XYSeries dst,
                                                           Predicate<ElectrodeDescription> tester) {
        var ret = new ArrayList<ElectrodeDescription>(src.size());
        src.transferData(dst, it -> {
            var e = (ElectrodeDescription) it.external();
            assert e != null;
            if (tester.test(e)) {
                ret.add(e);
                return true;
            }
            return false;
        });
        return ret;
    }

    private static List<ElectrodeDescription> copyData(XYSeries src,
                                                       XYSeries dst,
                                                       Predicate<ElectrodeDescription> tester) {
        var ret = new ArrayList<ElectrodeDescription>(src.size());
        src.copyData(dst, it -> {
            var e = (ElectrodeDescription) it.external();
            assert e != null;
            if (tester.test(e)) {
                ret.add(e);
                return true;
            }
            return false;
        });
        return ret;
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
    }

    public void clearChannelmap() {
        log.debug("clearChannelmap");
        channelmap = null;
        blueprint = null;

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
        this.blueprint = new Blueprint<>(probe, channelmap, blueprint).electrodes();

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
            for (var entry : electrodes.entrySet()) {
                var code = probe.stateOf(entry.getKey()).orElse(-1);

                var set = blueprint.stream().filter(it -> it.state() == code);
                setSeries(entry.getValue(), set);
            }

        } else {
            log.debug("updateElectrode(clear)");
            for (var series : electrodes.values()) {
                series.clearData();
            }
            for (var series : captured.values()) {
                series.clearData();
            }
        }

        clearHighlight();

        var scatter = getChart();
        scatter.setVerticalZeroLineVisible(blueprint == null);
        scatter.setHorizontalZeroLineVisible(blueprint == null);

        interaction.repaint();
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
        for (var state : this.electrodes.keySet()) {
            ret.addAll(getCaptured(state, reset));
        }

        if (reset && !ret.isEmpty() && !Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
        return ret;
    }

    public List<ElectrodeDescription> getCaptured(int state, boolean reset) {
        var name = probe.stateOf(state);
        if (name == null) throw new IllegalArgumentException();
        var ret = getCaptured(name, reset);
        if (reset && !ret.isEmpty() && !Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
        return ret;
    }

    public List<ElectrodeDescription> getCaptured(String name, boolean reset) {
        var src = captured.get(name);
        var dst = electrodes.get(name);
        if (src == null || dst == null) throw new IllegalArgumentException();
        var ret = transferData(src, reset ? dst : null);
        if (reset && !ret.isEmpty() && !Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
        return ret;
    }

    public void setCaptured(List<ElectrodeDescription> electrodes) {
        var s = new HashSet<>(electrodes);
        for (var state : this.electrodes.keySet()) {
            setCaptured(state, s);
        }
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
    }

    private void setCaptured(String name, Set<ElectrodeDescription> set) {
        var src = electrodes.get(name);
        var dst = captured.get(name);
        if (src == null || dst == null) throw new IllegalArgumentException();
        transferData(src, dst, set::contains);
    }

    public void unsetCaptured(List<ElectrodeDescription> electrodes) {
        var s = new HashSet<>(electrodes);
        for (var state : this.electrodes.keySet()) {
            unsetCaptured(state, s);
        }
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
    }

    private void unsetCaptured(String name, Set<ElectrodeDescription> set) {
        var src = captured.get(name);
        var dst = electrodes.get(name);
        if (src == null || dst == null) throw new IllegalArgumentException();
        transferData(src, dst, set::contains);
    }

    public void clearCaptured() {
        getCaptured(true);
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
    }

    public List<ElectrodeDescription> getHighlighted() {
        return probe.copyElectrodes(highlighted.data().map(it -> (ElectrodeDescription) it.external()).toList());
    }

    public void setHighlight(List<ElectrodeDescription> electrodes, boolean includeInvalid) {
        var chmap = this.channelmap;
        log.trace("setHighlight {} electrodes", electrodes.size());

        if (includeInvalid && chmap != null) {
            electrodes = probe.getInvalidElectrodes(chmap, electrodes, probe.allElectrodes(chmap));
            log.trace("setHighlight {} invalid electrodes", electrodes.size());
        }

        var s = new HashSet<>(electrodes);

        for (var state : this.electrodes.keySet()) {
            setHighlight(state, s);
        }

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
    }

    private void setHighlight(String name, Set<ElectrodeDescription> set) {
        var src = electrodes.get(name);
        var dst = highlighted;
        if (src == null) throw new IllegalArgumentException();
        copyData(src, dst, set::contains);
    }

    public void clearHighlight() {
        highlighted.clearData();
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(interaction::repaint);
        }
    }

    /*=================*
     * state save/load *
     *=================*/

    @JsonRootName("ProbeView")
    public record ProbeViewState(
      @JsonProperty(value = "x_axis", index = 0, required = true) double[] x,
      @JsonProperty(value = "y_axis", index = 1, required = true) double[] y
    ) {
        public ProbeViewState(double x1, double x2, double y1, double y2) {
            this(new double[]{x1, x2}, new double[]{y1, y2});
        }

        public ProbeViewState(AxesBounds bounds) {
            this(new double[]{bounds.xLower(), bounds.xUpper()},
              new double[]{bounds.yLower(), bounds.yUpper()});
        }
    }

    class ProbeViewStateListener implements StateView<ProbeViewState> {

        @Override
        public ProbeViewState getState() {
            log.debug("save");
            return new ProbeViewState(getAxesBounds());
        }

        @Override
        public void restoreState(@Nullable ProbeViewState state) {
            if (state == null) return;

            log.debug("restore");
            try {
                var bounds = new AxesBounds(
                  state.x[0], state.x[1],
                  state.y[0], state.y[1]
                );
                setAxesBoundaries(bounds);
            } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                log.warn("restore from bad config.json", e);
            }
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
        var ret = new ArrayList<ElectrodeDescription>();

        clearCaptured();

        for (var state : electrodes.keySet()) {
            var src = electrodes.get(state);
            var dst = captured.get(state);
            if (src != null && dst != null) {
                src.transferData(dst, xy -> {
                    var t = e.bounds.contains(xy.x(), xy.y());
                    if (t) {
                        ret.add((ElectrodeDescription) xy.external());
                    }
                    return t;
                });
            }
        }

        log.trace("captured {} electrodes", ret.size());

        if (ret.isEmpty()) {
            clearHighlight();
        } else {
            setHighlight(ret, true);
        }

        interaction.repaint();
    }

}
