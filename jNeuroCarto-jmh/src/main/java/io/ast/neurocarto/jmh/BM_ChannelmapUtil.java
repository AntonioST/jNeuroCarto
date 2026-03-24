package io.ast.neurocarto.jmh;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.ast.jneurocarto.probe_npx.NpxProbeType;

/// last run 2026/03/24
/// ```
/// Benchmark                          (code)  Mode  Cnt     Score     Error  Units
/// BM_ChannelmapUtil.measureCUP_c2e        0  avgt   25    34.191 ±   9.069  us/op
/// BM_ChannelmapUtil.measureCUP_c2e       21  avgt   25    67.824 ±  13.365  us/op
/// BM_ChannelmapUtil.measureCUP_c2e       24  avgt   25    46.817 ±   6.635  us/op
/// BM_ChannelmapUtil.measureCUP_c2ea       0  avgt   25    28.955 ±   3.517  us/op
/// BM_ChannelmapUtil.measureCUP_c2ea      21  avgt   25    56.299 ±  10.194  us/op
/// BM_ChannelmapUtil.measureCUP_c2ea      24  avgt   25    48.250 ±   9.411  us/op
/// BM_ChannelmapUtil.measureCUP_e2cb       0  avgt   25    67.209 ±  11.911  us/op
/// BM_ChannelmapUtil.measureCUP_e2cb      21  avgt   25    79.628 ±   5.832  us/op
/// BM_ChannelmapUtil.measureCUP_e2cb      24  avgt   25   100.835 ±  12.196  us/op
/// BM_ChannelmapUtil.measureCUP_e2cr       0  avgt   25    26.208 ±   9.279  us/op
/// BM_ChannelmapUtil.measureCUP_e2cr      21  avgt   25    20.447 ±   9.500  us/op
/// BM_ChannelmapUtil.measureCUP_e2cr      24  avgt   25    32.042 ±  35.253  us/op
/// BM_ChannelmapUtil.measureCUP_e2xy       0  avgt   25    67.626 ±   9.025  us/op
/// BM_ChannelmapUtil.measureCUP_e2xy      21  avgt   25    82.982 ±   4.174  us/op
/// BM_ChannelmapUtil.measureCUP_e2xy      24  avgt   25    83.257 ±   8.423  us/op
/// BM_ChannelmapUtil.measureCUV_c2e        0  avgt   25   768.752 ± 741.951  us/op
/// BM_ChannelmapUtil.measureCUV_c2e       21  avgt   25  2606.280 ± 617.427  us/op
/// BM_ChannelmapUtil.measureCUV_c2e       24  avgt   25  2122.728 ± 447.581  us/op
/// BM_ChannelmapUtil.measureCUV_c2ea       0  avgt   25   860.348 ± 893.279  us/op
/// BM_ChannelmapUtil.measureCUV_c2ea      21  avgt   25  2144.040 ± 750.158  us/op
/// BM_ChannelmapUtil.measureCUV_c2ea      24  avgt   25  2168.818 ± 444.696  us/op
/// BM_ChannelmapUtil.measureCUV_e2cb       0  avgt   25   645.708 ± 699.410  us/op
/// BM_ChannelmapUtil.measureCUV_e2cb      21  avgt   25   784.376 ± 187.834  us/op
/// BM_ChannelmapUtil.measureCUV_e2cb      24  avgt   25   570.412 ±  93.003  us/op
/// BM_ChannelmapUtil.measureCUV_e2cr       0  avgt   25   663.541 ± 740.052  us/op
/// BM_ChannelmapUtil.measureCUV_e2cr      21  avgt   25   806.671 ± 871.986  us/op
/// BM_ChannelmapUtil.measureCUV_e2cr      24  avgt   25   692.282 ± 750.826  us/op
/// BM_ChannelmapUtil.measureCUV_e2xy       0  avgt   25   453.488 ± 157.907  us/op
/// BM_ChannelmapUtil.measureCUV_e2xy      21  avgt   25   624.092 ± 123.560  us/op
/// BM_ChannelmapUtil.measureCUV_e2xy      24  avgt   25   611.556 ± 186.810  us/op
/// ```
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 5, timeUnit = TimeUnit.MICROSECONDS)
public class BM_ChannelmapUtil {

    @State(Scope.Benchmark)
    public static class Shared {
        @Param({"0", "21", "24"/*, "1110", "2020", "3010", "3020"*/})
        public int code;

        NpxProbeType type;
        int[] shank;
        int[] electrodes;
        int[][] channels;
        ChannelMapUtilValue.CB[] channels_value;

        @Setup
        public synchronized void setup() {
            type = NpxProbeType.of(code);
            var ne = type.nElectrodePerShank();
            shank = new int[ne];
            for (int i = 0; i < ne; i++) {
                shank[i] = 0;
            }
            electrodes = new int[ne];
            for (int i = 0; i < ne; i++) {
                electrodes[i] = i;
            }
            channels = ChannelMapUtilPlain.e2cb(type, 0, electrodes);
            channels_value = ChannelMapUtilValue.e2cb(type, 0, electrodes);
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measure_e2cr_plain(Shared shared) {
        return ChannelMapUtilPlain.e2cr(shared.type, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public ChannelMapUtilValue.CR[] measure_e2cr_value(Shared shared) {
        return ChannelMapUtilValue.e2cr(shared.type, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measure_e2cr_vector(Shared shared) {
        return ChannelMapUtilVec.e2cr(shared.type, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measure_e2xy_plain(Shared shared) {
        return ChannelMapUtilPlain.e2xy(shared.type, 0, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public ChannelMapUtilValue.XY[] measure_e2xy_value(Shared shared) {
        return ChannelMapUtilValue.e2xy(shared.type, 0, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measure_e2xy_vector(Shared shared) {
        return ChannelMapUtilVec.e2xy(shared.type, 0, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measure_e2cb_plain(Shared shared) {
        return ChannelMapUtilPlain.e2cb(shared.type, 0, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public ChannelMapUtilValue.CB[] measure_e2cb_value(Shared shared) {
        return ChannelMapUtilValue.e2cb(shared.type, 0, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measure_e2cb_vector(Shared shared) {
        return ChannelMapUtilVec.e2cb(shared.type, 0, shared.electrodes);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[] measure_c2e_plain(Shared shared) {
        return ChannelMapUtilPlain.c2e(shared.type, shared.channels, 0);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[] measure_c2ea_plain(Shared shared) {
        return ChannelMapUtilPlain.c2e(shared.type, shared.channels, shared.shank);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[] measure_c2e_value(Shared shared) {
        return ChannelMapUtilValue.c2e(shared.type, shared.channels_value, 0);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[] measure_c2ea_value(Shared shared) {
        return ChannelMapUtilValue.c2e(shared.type, shared.channels_value, shared.shank);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[] measure_c2e_vector(Shared shared) {
        return ChannelMapUtilVec.c2e(shared.type, shared.channels, 0);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[] measure_c2ea_vector(Shared shared) {
        return ChannelMapUtilVec.c2e(shared.type, shared.channels, shared.shank);
    }
}
