package io.ast.neurocarto.probe_npx.jmh;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.ast.jneurocarto.probe_npx.NpxProbeType;

/// last run 2025/05/29
/// {@snippet lang = "TEXT":
/// Benchmark                          (code)  Mode  Cnt    Score     Error  Units
/// BM_ChannelmapUtil.measureCUP_e2cb       0  avgt   25   69.227 ±  28.421  us/op
/// BM_ChannelmapUtil.measureCUP_e2cb      21  avgt   25  122.995 ±  35.008  us/op
/// BM_ChannelmapUtil.measureCUP_e2cb      24  avgt   25   78.227 ±   5.446  us/op
/// BM_ChannelmapUtil.measureCUP_e2cr       0  avgt   25   13.076 ±   5.522  us/op
/// BM_ChannelmapUtil.measureCUP_e2cr      21  avgt   25   15.824 ±   6.873  us/op
/// BM_ChannelmapUtil.measureCUP_e2cr      24  avgt   25   40.956 ±  47.901  us/op
/// BM_ChannelmapUtil.measureCUP_e2xy       0  avgt   25   80.659 ±  40.325  us/op
/// BM_ChannelmapUtil.measureCUP_e2xy      21  avgt   25   77.323 ±  30.548  us/op
/// BM_ChannelmapUtil.measureCUP_e2xy      24  avgt   25   75.798 ±  32.657  us/op
/// BM_ChannelmapUtil.measureCUV_e2cb       0  avgt   25  218.698 ±  37.952  us/op
/// BM_ChannelmapUtil.measureCUV_e2cb      21  avgt   25  639.439 ± 128.971  us/op
/// BM_ChannelmapUtil.measureCUV_e2cb      24  avgt   25  551.666 ± 125.078  us/op
/// BM_ChannelmapUtil.measureCUV_e2cr       0  avgt   25  256.231 ±  54.815  us/op
/// BM_ChannelmapUtil.measureCUV_e2cr      21  avgt   25  303.915 ±  98.987  us/op
/// BM_ChannelmapUtil.measureCUV_e2cr      24  avgt   25  332.022 ±  88.029  us/op
/// BM_ChannelmapUtil.measureCUV_e2xy       0  avgt   25  380.426 ±  85.346  us/op
/// BM_ChannelmapUtil.measureCUV_e2xy      21  avgt   25  456.188 ±  60.662  us/op
/// BM_ChannelmapUtil.measureCUV_e2xy      24  avgt   25  483.141 ± 106.215  us/op
///}
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MICROSECONDS)
public class BM_ChannelmapUtil {

    @State(Scope.Benchmark)
    public static class Shared {
        @Param({"0", "21", "24"})
        public int code;

        NpxProbeType type;
        int[] electrode;

        @Setup
        public synchronized void setup() {
            type = NpxProbeType.of(code);
            var ne = type.nElectrodePerShank();
            electrode = new int[ne];
            for (int i = 0; i < ne; i++) {
                electrode[i] = i;
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measureCUP_e2cr(Shared shared) {
        return ChannelMapUtilPlain.e2cr(shared.type, shared.electrode);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measureCUV_e2cr(Shared shared) {
        return ChannelMapUtilVec.e2cr(shared.type, shared.electrode);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measureCUP_e2xy(Shared shared) {
        return ChannelMapUtilPlain.e2xy(shared.type, 0, shared.electrode);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measureCUV_e2xy(Shared shared) {
        return ChannelMapUtilVec.e2xy(shared.type, 0, shared.electrode);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measureCUP_e2cb(Shared shared) {
        return ChannelMapUtilPlain.e2cb(shared.type, 0, shared.electrode);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measureCUV_e2cb(Shared shared) {
        return ChannelMapUtilVec.e2cb(shared.type, 0, shared.electrode);
    }
}
