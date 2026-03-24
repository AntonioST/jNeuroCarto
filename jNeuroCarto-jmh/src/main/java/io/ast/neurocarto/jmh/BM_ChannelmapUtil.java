package io.ast.neurocarto.jmh;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.ast.jneurocarto.probe_npx.NpxProbeType;

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
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[][] measure_e2cr_plain(Shared shared) {
        return ChannelMapUtilPlain.e2cr(shared.type, shared.electrodes);
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
    public int[] measure_c2e_vector(Shared shared) {
        return ChannelMapUtilVec.c2e(shared.type, shared.channels, 0);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public int[] measure_c2ea_vector(Shared shared) {
        return ChannelMapUtilVec.c2e(shared.type, shared.channels, shared.shank);
    }
}
