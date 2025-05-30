package io.ast.neurocarto.jmh;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.ast.jneurocarto.core.blueprint.BlueprintMask;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;

/// last run 2025/05/30
/// ```
/// Benchmark                                 (size)  Mode  Cnt    Score    Error  Units
/// BM_BlueprintToolkitReduce.reduceBaseline      10  avgt   25    0.047 ±  0.012  ms/op
/// BM_BlueprintToolkitReduce.reduceBaseline      20  avgt   25    0.556 ±  0.016  ms/op
/// BM_BlueprintToolkitReduce.reduceBaseline      50  avgt   25   27.258 ±  0.425  ms/op
/// BM_BlueprintToolkitReduce.reduceBaseline     100  avgt   25  327.562 ±  6.857  ms/op
/// BM_BlueprintToolkitReduce.reduceByIndex       10  avgt   25    0.031 ±  0.001  ms/op
/// BM_BlueprintToolkitReduce.reduceByIndex       20  avgt   25    0.495 ±  0.014  ms/op
/// BM_BlueprintToolkitReduce.reduceByIndex       50  avgt   25   25.103 ±  0.490  ms/op
/// BM_BlueprintToolkitReduce.reduceByIndex      100  avgt   25  286.007 ±  3.384  ms/op
/// BM_BlueprintToolkitReduce.reduceByMask        10  avgt   25    0.032 ±  0.001  ms/op
/// BM_BlueprintToolkitReduce.reduceByMask        20  avgt   25    0.472 ±  0.013  ms/op
/// BM_BlueprintToolkitReduce.reduceByMask        50  avgt   25   24.869 ±  0.806  ms/op
/// BM_BlueprintToolkitReduce.reduceByMask       100  avgt   25  296.640 ± 10.174  ms/op
///```
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class BM_BlueprintToolkitReduce {

    @State(Scope.Benchmark)
    public static class Shared {
        @Param({"10", "20", "50", "100"})
        public int size;

        BlueprintToolkit<?> toolkit;
        int[] blueprint;

        @Setup
        public synchronized void setup() {
            toolkit = BlueprintToolkit.dummy(1, size, size);
            var total = size * size;
            blueprint = new int[total];
            for (int i = 0; i < total; i++) {
                var x = i % size;
                var y = i / size;
                blueprint[i] = (0 < x && x < size - 1) && (0 < y && y < size - 1) ? 1 : 0;
            }

            toolkit.from(blueprint);
        }
    }

    @Benchmark
    public int[] reduceBaseline(Shared shared) {
        return reduceBaseline(shared, 1, new BlueprintToolkit.AreaChange(1, 1, 1, 1));
    }

    @Benchmark
    public int[] reduceByIndex(Shared shared) {
        return reduceByIndex(shared, 1, new BlueprintToolkit.AreaChange(1, 1, 1, 1));
    }

    @Benchmark
    public int[] reduceByMask(Shared shared) {
        return reduceByMask(shared, 1, new BlueprintToolkit.AreaChange(1, 1, 1, 1));
    }

    public static int[] reduceBaseline(Shared shared, int category,
                                       BlueprintToolkit.AreaChange step) {
        return reduceBaseline(shared.toolkit, shared.blueprint.clone(), shared.blueprint,
          category, step, 0, BlueprintToolkit.AreaThreshold.ALL);
    }

    public static int[] reduceByIndex(Shared shared, int category,
                                      BlueprintToolkit.AreaChange step) {
        return reduceByIndex(shared.toolkit, shared.blueprint.clone(), shared.blueprint,
          category, step, 0, BlueprintToolkit.AreaThreshold.ALL);
    }

    public static int[] reduceByMask(Shared shared, int category,
                                     BlueprintToolkit.AreaChange step) {
        return reduceByMask(shared.toolkit, shared.blueprint.clone(), shared.blueprint,
          category, step, 0, BlueprintToolkit.AreaThreshold.ALL);
    }

    public static int[] reduceBaseline(BlueprintToolkit toolkit,
                                       int[] output, int[] blueprint, int category,
                                       BlueprintToolkit.AreaChange step,
                                       int value,
                                       BlueprintToolkit.AreaThreshold threshold) {
        if (step.isZero() || toolkit.length() == 0) return output;

        var clustering = toolkit.findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var index = clustering.indexGroup();
        var move = new int[index.length];

        for (var m : step) {
            int size = toolkit.moveIndex(move, index, m);
            Arrays.sort(move, 0, size);

            for (int i : index) {
                if (Arrays.binarySearch(move, i) < 0) { // `i` does not live in moved area
                    output[i] = value;
                }
            }
        }

        return output;
    }

    public static int[] reduceByIndex(BlueprintToolkit toolkit,
                                      int[] output, int[] blueprint, int category,
                                      BlueprintToolkit.AreaChange step,
                                      int value,
                                      BlueprintToolkit.AreaThreshold threshold) {
        if (step.isZero() || toolkit.length() == 0) return output;

        var clustering = toolkit.findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var index = clustering.indexGroup();
        var move = new int[index.length];

        int size;
        if (step.left() > 0) {
            size = toolkit.moveIndex(move, index, new BlueprintToolkit.Movement(step.left(), 0)); // move right
            Arrays.sort(move, 0, size);
            for (int i : index) {
                if (Arrays.binarySearch(move, i) < 0) { // `i` does not live in moved area
                    output[i] = value;
                }
            }
        }

        if (step.right() > 0) {
            size = toolkit.moveIndex(move, index, new BlueprintToolkit.Movement(-step.right(), 0)); // move left
            Arrays.sort(move, 0, size);
            for (int i : index) {
                if (Arrays.binarySearch(move, i) < 0) { // `i` does not live in moved area
                    output[i] = value;
                }
            }
        }

        if (step.up() > 0) {
            size = toolkit.moveIndex(move, index, new BlueprintToolkit.Movement(0, -step.up())); // move down
            Arrays.sort(move, 0, size);
            for (int i : index) {
                if (Arrays.binarySearch(move, i) < 0) { // `i` does not live in moved area
                    output[i] = value;
                }
            }
        }

        if (step.down() > 0) {
            size = toolkit.moveIndex(move, index, new BlueprintToolkit.Movement(0, step.down())); // move up
            Arrays.sort(move, 0, size);
            for (int i : index) {
                if (Arrays.binarySearch(move, i) < 0) { // `i` does not live in moved area
                    output[i] = value;
                }
            }
        }

        return output;
    }

    public static int[] reduceByMask(BlueprintToolkit toolkit,
                                     int[] output, int[] blueprint, int category,
                                     BlueprintToolkit.AreaChange step,
                                     int value,
                                     BlueprintToolkit.AreaThreshold threshold) {
        if (step.isZero() || toolkit.length() == 0) return output;

        var clustering = toolkit.findClustering(blueprint, category, true);
        for (var group : clustering.groups()) {
            if (!threshold.test(clustering.groupCount(group))) {
                clustering.removeGroup(group);
            }
        }

        var from = clustering.maskGroup();
        var mark = new BlueprintMask(from);
        if (step.left() > 0) {
            var move = toolkit.moveMask(from, new BlueprintToolkit.Movement(step.left(), 0)); // move right
            mark.iand(move);
        }

        if (step.right() > 0) {
            var move = toolkit.moveMask(from, new BlueprintToolkit.Movement(-step.right(), 0)); // move left
            mark.iand(move);
        }

        if (step.up() > 0) {
            var move = toolkit.moveMask(from, new BlueprintToolkit.Movement(0, -step.up())); // move down
            mark.iand(move);
        }

        if (step.down() > 0) {
            var move = toolkit.moveMask(from, new BlueprintToolkit.Movement(0, step.down())); // move up
            mark.iand(move);
        }

        from.idiff(mark).fill(output, value);

        return output;
    }
}
