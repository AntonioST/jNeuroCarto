package io.ast.neurocarto.probe_npx.jmh;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.ast.jneurocarto.probe_npx.ChannelMapUtilPlain;
import io.ast.jneurocarto.probe_npx.ChannelMapUtilVec;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MICROSECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MICROSECONDS)
public class ChannelmapUtils {

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
