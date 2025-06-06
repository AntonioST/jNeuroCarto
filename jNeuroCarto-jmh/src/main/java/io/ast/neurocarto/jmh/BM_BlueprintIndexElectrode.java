package io.ast.neurocarto.jmh;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class BM_BlueprintIndexElectrode {

    @Param({"0", "21", "24"})
    public int code;

    NpxProbeType type;
    Blueprint<ChannelMap> blueprint;

    int[] shank;
    int[] posx;
    int[] posy;

    int[] s;
    int[] x;
    int[] y;

    @Setup(Level.Trial)
    public void setUp() {
        type = NpxProbeType.of(code);
        var probe = new NpxProbeDescription();
        blueprint = new Blueprint<>(probe, new ChannelMap(type));
        var tool = new BlueprintToolkit<>(blueprint);
        shank = tool.shank();
        posx = tool.posx();
        posy = tool.posy();

        record T(int index, double value) {
        }

        var length = 1000;
        var t = new T[length];
        for (int i = 0; i < length; i++) {
            t[i] = new T(i, Math.random());
        }
        Arrays.sort(t, Comparator.comparingDouble(T::value));

        s = new int[length];
        x = new int[length];
        y = new int[length];

        for (int i = 0; i < length; i++) {
            var j = t[i].index;
            s[i] = j % type.nShank();
            x[i] = j % type.nColumnPerShank();
            y[i] = j % type.nRowPerShank();
        }
    }

    @Benchmark
    public void measureIndexElectrodeSXY(Blackhole bh) {
        var s = this.s;
        var x = this.x;
        var y = this.y;

        for (int e = 0, length = s.length; e < length; e++) {
            bh.consume(indexSXY(s[e], x[e], y[e]));
        }
    }

    public int indexSXY(int s, int x, int y) {
        var shank = this.shank;
        var posx = this.posx;
        var posy = this.posy;
        for (int i = 0, length = shank.length; i < length; i++) {
            if (shank[i] == s && posx[i] == x && posy[i] == y) return i;
        }
        return -1;
    }

    @Benchmark
    public void measureIndexElectrodeYSX(Blackhole bh) {
        var s = this.s;
        var x = this.x;
        var y = this.y;

        for (int e = 0, length = s.length; e < length; e++) {
            bh.consume(indexYSX(s[e], x[e], y[e]));
        }
    }

    public int indexYSX(int s, int x, int y) {
        var shank = this.shank;
        var posx = this.posx;
        var posy = this.posy;
        for (int i = 0, length = shank.length; i < length; i++) {
            if (posy[i] == y && shank[i] == s && posx[i] == x) return i;
        }
        return -1;
    }
}
