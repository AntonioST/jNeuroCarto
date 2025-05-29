package io.ast.jneurocarto.probe_npx;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ElectrodeSelector;
import io.ast.jneurocarto.core.blueprint.Blueprint;
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;

@NullMarked
public final class ChannelMaps {

    private ChannelMaps() {
        throw new RuntimeException();
    }

    public static ChannelMap npx24SingleShank(int shank, double row) {
        var type = NpxProbeType.NP24;
        try {
            return npx24SingleShank(shank, (int) (row / type.spacePerRow()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("row over range : ")) {
                throw new IllegalArgumentException("row over range : " + row + " um", e);
            } else {
                throw e;
            }
        }
    }

    public static ChannelMap npx24SingleShank(int shank, int row) {
        var type = NpxProbeType.NP24;
        if (!(0 <= shank && shank < type.nShank())) {
            throw new IllegalArgumentException("shank over range : " + shank);
        }

        var nc = type.nColumnPerShank();
        var nr = type.nChannel() / nc;
        if (!(0 <= row && row + nr < type.nRowPerShank())) {
            throw new IllegalArgumentException("row over range : " + row);
        }

        var ret = new ChannelMap(type);
        for (int r = 0; r < nr; r++) {
            for (int c = 0; c < nc; c++) {
                try {
                    ret.addElectrode(shank, c, r + row);
                } catch (ChannelHasBeenUsedException e) {
                }
            }
        }

        return ret;
    }

    public static ChannelMap npx24Stripe(double row) {
        var type = NpxProbeType.NP24;
        try {
            return npx24Stripe((int) (row / type.spacePerRow()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("row over range : ")) {
                throw new IllegalArgumentException("row over range : " + row + " um", e);
            } else {
                throw e;
            }
        }
    }

    public static ChannelMap npx24Stripe(int row) {
        var type = NpxProbeType.NP24;
        var ns = type.nShank();
        var nc = type.nColumnPerShank();
        var nr = type.nChannel() / (nc * ns);
        if (!(0 <= row && row + nr < type.nRowPerShank())) {
            throw new IllegalArgumentException("row over range : " + row);
        }

        var ret = new ChannelMap(type);
        for (int s = 0; s < ns; s++) {
            for (int r = 0; r < nr; r++) {
                for (int c = 0; c < nc; c++) {
                    try {
                        ret.addElectrode(s, c, r + row);
                    } catch (ChannelHasBeenUsedException e) {
                    }
                }
            }
        }

        return ret;
    }

    public static ChannelMap npx24HalfDensity(int shank, double row) {
        var type = NpxProbeType.NP24;
        return npx24HalfDensity(shank, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24HalfDensity(int s1, int s2, double row) {
        var type = NpxProbeType.NP24;
        return npx24HalfDensity(s1, s2, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24HalfDensity(int shank, int row) {
        var type = NpxProbeType.NP24;
        if (!(0 <= shank && shank < type.nShank())) {
            throw new IllegalArgumentException("shank over range : " + shank);
        }

        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 2) {
            addElectrode(ret, shank, 0, r + row);
            addElectrode(ret, shank, 1, r + row + 1);
        }
        for (int r = 192; r < 384; r += 2) {
            addElectrode(ret, shank, 1, r + row);
            addElectrode(ret, shank, 0, r + row + 1);
        }

        return ret;
    }

    public static ChannelMap npx24HalfDensity(int s1, int s2, int row) {
        var type = NpxProbeType.NP24;
        var ns = type.nShank();
        if (!(0 <= s1 && s1 < ns)) {
            throw new IllegalArgumentException("shank (s1) over range : " + s1);
        }
        if (!(0 <= s2 && s2 < ns)) {
            throw new IllegalArgumentException("shank (s2) over range : " + s2);
        }

        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 2) {
            addElectrode(ret, s1, 0, r + row);
            addElectrode(ret, s1, 1, r + row + 1);
        }
        for (int r = 0; r < 192; r += 2) {
            addElectrode(ret, s2, 1, r + row);
            addElectrode(ret, s2, 0, r + row + 1);
        }

        return ret;
    }

    public static ChannelMap npx24QuarterDensity(double row) {
        var type = NpxProbeType.NP24;
        return npx24QuarterDensity((int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24QuarterDensity(int shank, double row) {
        var type = NpxProbeType.NP24;
        return npx24QuarterDensity(shank, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24QuarterDensity(int s1, int s2, double row) {
        var type = NpxProbeType.NP24;
        return npx24QuarterDensity(s1, s2, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24QuarterDensity(int row) {
        var type = NpxProbeType.NP24;
        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 4) {
            addElectrode(ret, 0, 0, r + row);
            addElectrode(ret, 0, 1, r + row + 2);
            addElectrode(ret, 1, 1, r + row);
            addElectrode(ret, 1, 0, r + row + 2);
            addElectrode(ret, 2, 0, r + row + 1);
            addElectrode(ret, 2, 1, r + row + 3);
            addElectrode(ret, 3, 1, r + row + 1);
            addElectrode(ret, 3, 0, r + row + 3);
        }

        return ret;
    }

    public static ChannelMap npx24QuarterDensity(int shank, int row) {
        var type = NpxProbeType.NP24;
        if (!(0 <= shank && shank < type.nShank())) {
            throw new IllegalArgumentException("shank over range : " + shank);
        }

        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 4) {
            addElectrode(ret, shank, 0, r + row);
            addElectrode(ret, shank, 1, r + row + 2);
        }
        for (int r = 192; r < 384; r += 4) {
            addElectrode(ret, shank, 1, r + row);
            addElectrode(ret, shank, 0, r + row + 2);
        }
        for (int r = 384; r < 576; r += 4) {
            addElectrode(ret, shank, 0, r + row + 1);
            addElectrode(ret, shank, 1, r + row + 3);
        }
        for (int r = 576; r < 768; r += 4) {
            addElectrode(ret, shank, 1, r + row + 1);
            addElectrode(ret, shank, 0, r + row + 3);
        }

        return ret;
    }

    public static ChannelMap npx24QuarterDensity(int s1, int s2, int row) {
        var type = NpxProbeType.NP24;
        var ns = type.nShank();
        if (!(0 <= s1 && s1 < ns)) {
            throw new IllegalArgumentException("shank (s1) over range : " + s1);
        }
        if (!(0 <= s2 && s2 < ns)) {
            throw new IllegalArgumentException("shank (s2) over range : " + s2);
        }

        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 4) {
            addElectrode(ret, s1, 0, r + row);
            addElectrode(ret, s1, 1, r + row + 2);
            addElectrode(ret, s2, 1, r + row + 1);
            addElectrode(ret, s2, 0, r + row + 3);
        }
        for (int r = 192; r < 384; r += 4) {
            addElectrode(ret, s1, 1, r + row);
            addElectrode(ret, s1, 0, r + row + 2);
            addElectrode(ret, s2, 0, r + row + 1);
            addElectrode(ret, s2, 1, r + row + 3);
        }

        return ret;
    }

    public static ChannelMap npx24OneEightDensity(double row) {
        var type = NpxProbeType.NP24;
        return npx24OneEightDensity((int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24OneEightDensity(int row) {
        var type = NpxProbeType.NP24;

        var nc = type.nColumnPerShank();

        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 8) {
            addElectrode(ret, 0, 0, r + row);
            addElectrode(ret, 1, 0, r + row + 1);
            addElectrode(ret, 2, 0, r + row + 2);
            addElectrode(ret, 3, 0, r + row + 3);
            addElectrode(ret, 0, 1, r + row + 5);
            addElectrode(ret, 1, 1, r + row + 6);
            addElectrode(ret, 2, 1, r + row + 7);
            addElectrode(ret, 3, 1, r + row + 8);
        }
        for (int r = 192; r < 384; r += 8) {
            addElectrode(ret, 0, 1, r + row);
            addElectrode(ret, 1, 1, r + row + 1);
            addElectrode(ret, 2, 1, r + row + 2);
            addElectrode(ret, 3, 1, r + row + 3);
            addElectrode(ret, 0, 0, r + row + 5);
            addElectrode(ret, 1, 0, r + row + 6);
            addElectrode(ret, 2, 0, r + row + 7);
            addElectrode(ret, 3, 0, r + row + 8);
        }

        return ret;
    }

    private static void addElectrode(ChannelMap ret, int shank, int column, int row) {
        try {
            ret.addElectrode(shank, column, row);
        } catch (ChannelHasBeenUsedException | IllegalArgumentException e) {
        }
    }

    public static double requestElectrode(Blueprint<?> blueprint) {
        var tool = new BlueprintToolkit<>(blueprint);
        var s1 = tool.count(NpxProbeDescription.CATE_SET);
        s1 += tool.count(NpxProbeDescription.CATE_FULL);
        var s2 = tool.count(NpxProbeDescription.CATE_HALF);
        var s4 = tool.count(NpxProbeDescription.CATE_QUARTER);
        return (double) s1 + (double) (s2) / 2 + (double) (s4) / 4;
    }

    /**
     * @param area            area efficiency
     * @param channelComplete completed channel efficiency
     * @param used            used channel number
     * @param total           total channel number
     */
    public record Efficiency(double area, double channelComplete, int used, int total) {
        public double complete() {
            return (double) used / total;
        }

        public double incomplete() {
            return (double) (total - used) / total;
        }

        /**
         * {@return channel efficiency}
         */
        public double efficiency() {
            return channelComplete * incomplete();
        }
    }

    public static Efficiency channelEfficiency(Blueprint<ChannelMap> blueprint) {
        var chmap = Objects.requireNonNull(blueprint.channelmap(), "missing channelmap");
        var request = requestElectrode(blueprint);
        var total = chmap.nChannel();
        var unused = total - chmap.size();
        var channel = 0;
        var excluded = 0;

        var tool = new BlueprintToolkit<>(blueprint);
        var selected = tool.index();
        channel += tool.count(NpxProbeDescription.CATE_SET, selected);
        channel += tool.count(NpxProbeDescription.CATE_FULL, selected);
        channel += tool.count(NpxProbeDescription.CATE_HALF, selected);
        channel += tool.count(NpxProbeDescription.CATE_QUARTER, selected);
        excluded += tool.count(NpxProbeDescription.CATE_EXCLUDED, selected);

        var effA = request == 0 ? 0 : Math.max((double) channel / request, 0);
        var effC = effA == 0 ? 0 : Math.min(effA, 1 / effA);
        return new Efficiency(effA, effC, excluded + unused, total);
    }

    public static @Nullable ChannelMap selectBestEfficiencyResult(ChannelMap chmap,
                                                                  List<ElectrodeDescription> blueprint,
                                                                  String selector,
                                                                  int sampleTimes,
                                                                  int parallel) {
        var s = new NpxProbeDescription().newElectrodeSelector(selector);
        return selectBestEfficiencyResult(chmap, blueprint, s, sampleTimes, parallel);
    }

    /**
     * Sample each electrode selection result and select the one with the best channel efficiency metric.
     *
     * @param chmap       initial channelmap.
     * @param blueprint   blueprint
     * @param selector    electrode selector
     * @param sampleTimes sample times of electrode selection
     * @param parallel
     * @return a channelmap result with the highest channel efficiency. {@code null} if the thread is interrupt.
     * @throws RuntimeException if any {@link ExecutionException} is thrown.
     */
    public static @Nullable ChannelMap selectBestEfficiencyResult(ChannelMap chmap,
                                                                  List<ElectrodeDescription> blueprint,
                                                                  ElectrodeSelector selector,
                                                                  int sampleTimes,
                                                                  int parallel) {
        record Result(@Nullable ChannelMap result, double efficiency) implements Comparable<Result> {
            @Override
            public int compareTo(Result o) {
                return Double.compare(efficiency, o.efficiency);
            }

            static Result max(Result a, Result b) {
                return a.compareTo(b) >= 0 ? a : b;
            }
        }

        try (var scope = new AwaitAll<Result>(parallel)) {
            var desp = new NpxProbeDescription();
            var bp = new Blueprint<>(desp, chmap, blueprint);

            for (int i = 0; i < sampleTimes; i++) {
                scope.fork(() -> {
                    var newMap = selector.select(desp, chmap, blueprint);
                    if (desp.validateChannelmap(newMap)) {
                        var efficiency = channelEfficiency(new Blueprint<>(bp, newMap)).efficiency();
                        return new Result(newMap, efficiency);
                    } else {
                        return new Result(null, 0);
                    }
                });
            }

            bp.from(blueprint);
            var ret = new Result(chmap, channelEfficiency(bp).efficiency());
            return scope.join().result().stream()
              .reduce(ret, Result::max)
              .result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * @param <T> task return type.
     * @deprecated it is preview feature that it will be deprecated at java 25.
     */
    @SuppressWarnings("preview")
    @Deprecated
    private static class AwaitAll<T> extends StructuredTaskScope<T> {
        private final Semaphore semaphore;
        private final Queue<T> result = new LinkedTransferQueue<>();

        AwaitAll(int parallel) {
            int maxThreadCount;
            if (parallel < 0) {
                maxThreadCount = Runtime.getRuntime().availableProcessors();
            } else if (parallel == 0) {
                maxThreadCount = 1;
            } else {
                maxThreadCount = parallel;
            }
            semaphore = new Semaphore(maxThreadCount);
        }

        @Override
        public <U extends T> Subtask<U> fork(Callable<? extends U> task) {
            return super.fork(() -> {
                semaphore.acquireUninterruptibly();
                try {
                    return task.call();
                } finally {
                    semaphore.release();
                }
            });
        }

        @Override
        protected void handleComplete(Subtask<? extends T> subtask) {
            if (subtask.state() == Subtask.State.SUCCESS) {
                result.add(subtask.get());
            }
        }

        @Override
        public AwaitAll<T> join() throws InterruptedException {
            super.join();
            return this;
        }

        public List<T> result() {
            return new ArrayList<>(result);
        }
    }


    public record ProbabilityResult(
      int sampleTimes,
      int[] summation,
      int complete,
      double[] efficiency
    ) {

        public double[] probability() {
            var ret = new double[summation.length];
            for (int i = 0, length = ret.length; i < length; i++) {
                ret[i] = (double) summation[i] / sampleTimes;
            }
            return ret;
        }

        public double completeRate() {
            return (double) complete / sampleTimes;
        }

        public double maxEfficiency() {
            return Arrays.stream(efficiency).max().orElse(0);
        }

        public double meanEfficiency() {
            return Arrays.stream(efficiency).sum() / sampleTimes;
        }

        public double varEfficiency() {
            var mean = meanEfficiency();
            return Arrays.stream(efficiency).map(it -> Math.pow(it - mean, 2)).sum() / sampleTimes;
        }
    }

    public static @Nullable ProbabilityResult electrodeProbability(ChannelMap chmap,
                                                                   List<ElectrodeDescription> blueprint,
                                                                   String selector,
                                                                   int sampleTimes,
                                                                   int parallel) {
        var s = new NpxProbeDescription().newElectrodeSelector(selector);
        return electrodeProbability(chmap, blueprint, s, sampleTimes, parallel);
    }

    /**
     * @param chmap       initial channelmap. Do not count in the result.
     * @param blueprint   blueprint
     * @param selector    electrode selector
     * @param sampleTimes sample times of electrode selection
     * @param parallel
     * @return result. {@code null} if the thread is interrupt.
     * @throws RuntimeException if any {@link ExecutionException} is thrown.
     */
    public static @Nullable ProbabilityResult electrodeProbability(ChannelMap chmap,
                                                                   List<ElectrodeDescription> blueprint,
                                                                   ElectrodeSelector selector,
                                                                   int sampleTimes,
                                                                   int parallel) {
        record Result(int[] index, boolean complete, double efficiency) {
            public void sum(int[] summation) {
                for (var e : index) {
                    summation[e]++;
                }
            }
        }

        try (var scope = new AwaitAll<Result>(parallel)) {
            var desp = new NpxProbeDescription();
            var bp = new Blueprint<>(desp, chmap, blueprint);
            var tool = new BlueprintToolkit<>(bp);

            scope.fork(() -> {
                var newMap = selector.select(desp, chmap, blueprint);
                var index = tool.index(newMap);
                var complete = desp.validateChannelmap(newMap);
                var efficiency = channelEfficiency(bp).efficiency();
                return new Result(index, complete, efficiency);
            });

            var results = scope.join().result();
            var summation = new int[tool.length()];
            for (var result : results) {
                result.sum(summation);
            }
            var complete = (int) results.stream().filter(Result::complete).count();
            var efficiency = results.stream().mapToDouble(Result::efficiency).toArray();
            return new ProbabilityResult(sampleTimes, summation, complete, efficiency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static double[][] calculateElectrodeDensity(ChannelMap chmap, double dy, double smooth) {
        if (smooth < 0) throw new IllegalArgumentException();
        var type = chmap.type();
        var kernel = newSmoothKernel(smooth / dy);
        var density = new double[chmap.nShank()][];
        var npr = (int) (type.spacePerRow() / dy);
        var nr = npr * chmap.nRowPerShank();
        var shank = new double[nr];
        for (int i = 0, length = density.length; i < length; i++) {
            var output = shank.clone();
            density[i] = convolution(fill(chmap, i, npr, shank), kernel, output);
            Arrays.fill(shank, 0);
        }
        return density;
    }

    private static double[] newSmoothKernel(double std) {
        if (std < 0) throw new IllegalArgumentException();
        double[] kernel;
        if (std == 0) {
            return new double[]{1};
        } else {
            var n = (int) Math.floor(3 * std);
            kernel = new double[2 * n + 1];
            var var = 2 * std * std;
            var sum = 0.0;
            for (int i = 0; i <= n; i++) {
                var v = Math.exp(-i * i / var);
                kernel[n + i] = v;
                if (i > 0) {
                    kernel[n - i] = v;
                    sum += v + v;
                } else {
                    sum += v;
                }
            }
            for (int i = 0; i < kernel.length; i++) {
                kernel[i] /= sum;
            }
            return kernel;
        }
    }

    private static double[] fill(ChannelMap chmap, int shank, int npr, double[] count) {
        for (var e : chmap.channels()) {
            if (e != null && e.shank == shank) {
                count[e.row * npr]++;
            }
        }
        return count;
    }

    private static double[] convolution(double[] array, double[] kernel, double[] output) {
        int m = array.length;
        int n2 = kernel.length;
        int n = n2 / 2; // assume kernel is odd-sized and symmetric

        if (output.length != m) throw new IllegalArgumentException();

        for (int i = 0; i < m; i++) {
            double sum = 0;
            for (int j = 0; j < n2; j++) {
                int k = i + j - n; // center the kernel
                if (k >= 0 && k < m) {
                    sum += array[k] * kernel[j];
                }
            }
            output[i] = sum;
        }

        return output;
    }

    public static String printProbe(ChannelMap chmap) {
        return printProbe(chmap, false, false);
    }

    public static String printProbe(ChannelMap chmap, boolean truncate) {
        return printProbe(chmap, truncate, false);
    }

    public static String printProbe(ChannelMap chmap, boolean truncate, boolean um) {
        var sb = new StringBuilder();
        try {
            printProbe(sb, chmap, truncate, um);
        } catch (IOException e) {
        }
        return sb.toString();
    }

    public static void printProbe(PrintStream out, ChannelMap chmap) {
        printProbe(out, chmap, false, false);
    }

    public static void printProbe(PrintStream out, ChannelMap chmap, boolean truncate) {
        printProbe(out, chmap, truncate, false);
    }

    public static void printProbe(PrintStream out, ChannelMap chmap, boolean truncate, boolean um) {
        try {
            printProbe((Appendable) out, chmap, truncate, um);
        } catch (IOException e) {
        }
    }

    public static void printProbe(Appendable out, ChannelMap chmap) throws IOException {
        printProbe(out, chmap, false, false);
    }

    public static void printProbe(Appendable out, ChannelMap chmap, boolean truncate) throws IOException {
        printProbe(out, chmap, truncate, false);
    }

    public static void printProbe(Appendable out, ChannelMap chmap, boolean truncate, boolean um) throws IOException {
        var pus = PrintProbeUnicodeSymbols.getInstance();
        var type = chmap.type();
        var ur = pus.nr;
        var nr = type.nRowPerShank() / ur;

        var sr = um ? type.spacePerRow() : 1;
        String[] lines = new String[nr]; // line number
        for (int i = 0; i < nr; i++) {
            lines[i] = Integer.toString(ur * i * sr);
        }
        int maxNrLength = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);

        var body = printProbeRaw(chmap, pus);

        // body
        boolean checkTruncate = truncate;
        for (int r = nr - 1; r >= 0; r--) {
            var row = body.get(r);
            //noinspection AssignmentUsedAsCondition
            if (checkTruncate && (checkTruncate = pus.isBlank(row))) continue;

            for (int i = 0; i < maxNrLength - lines[r].length(); i++) out.append(' ');
            out.append(lines[r]);
            out.append(row);
            out.append('\n');
        }

        // tip
        for (int i = 0; i < maxNrLength; i++) out.append(' ');
        for (int i = 0; i < type.nShank(); i++) {
            out.append(pus.tip);
        }
        out.append('\n');
    }


    public static String printProbe(List<ChannelMap> chmap) {
        return printProbe(chmap, false);
    }

    public static String printProbe(List<ChannelMap> chmap, boolean truncate) {
        var sb = new StringBuilder();
        try {
            printProbe(sb, chmap, truncate);
        } catch (IOException e) {
        }
        return sb.toString();
    }

    public static void printProbe(PrintStream out, List<ChannelMap> chmap) {
        printProbe(out, chmap, false);
    }

    public static void printProbe(PrintStream out, List<ChannelMap> chmap, boolean truncate) {
        try {
            printProbe((Appendable) out, chmap, truncate);
        } catch (IOException e) {
        }
    }

    public static void printProbe(Appendable out, List<ChannelMap> chmap) throws IOException {
        printProbe(out, chmap, false);
    }

    public static void printProbe(Appendable out, List<ChannelMap> chmap, boolean truncate) throws IOException {
        var pus = PrintProbeUnicodeSymbols.getInstance();
        var ur = pus.nr;
        var nr = chmap.stream().mapToInt(it -> it.nRowPerShank() / ur).max().orElse(0);
        String[] rows = new String[nr];
        for (int i = 0; i < nr; i++) {
            rows[i] = Integer.toString(ur * i);
        }
        int maxNrLength = Arrays.stream(rows).mapToInt(String::length).max().orElse(0);

        var bodies = chmap.stream().map(it -> printProbeRaw(it, pus)).toList();

        // body
        boolean checkTruncate = truncate;
        for (int r = nr - 1; r >= 0; r--) {
            int rr = r;
            if (checkTruncate) {
                checkTruncate = bodies.stream()
                  .map(it -> getRemappedRowContent(it, rr))
                  .allMatch(pus::isBlank);
                if (checkTruncate) continue;
            }

            for (int i = 0; i < maxNrLength - rows[r].length(); i++) out.append(' ');
            out.append(rows[r]);

            for (var body : bodies) {
                out.append(getRemappedRowContent(body, r));
                out.append("  ");
            }
            out.append('\n');
        }

        // tip
        for (int i = 0; i < maxNrLength; i++) out.append(' ');
        for (var m : chmap) {
            for (int i = 0; i < m.nShank(); i++) {
                out.append(pus.tip);
                out.append(' ');
            }
        }
        out.append('\n');
    }

    private static String getRemappedRowContent(List<String> content, int r) {
        if (r < content.size()) {
            return content.get(r);
        } else {
            return " ".repeat(content.get(0).length());
        }
    }

    enum PrintProbeUnicodeSymbols {
        S22(2, 2, '▕', '▏', " ╹ ", new char[]{
          // 0x02 0x08
          // 0x01 0x04
          ' ', '▖', '▘', '▌',
          '▗', '▄', '▚', '▙',
          '▝', '▞', '▀', '▛',
          '▐', '▟', '▜', '█',
        }),
        S42(4, 2, ' ', ' ', " ╷ ", new char[]{
          // 0x08 0x80
          // 0x04 0x40
          // 0x02 0x20
          // 0x01 0x10
          ' ', '⡀', '⠄', '⡄', '⠂', '⡂', '⠆', '⡆', '⠁', '⡁', '⠅', '⡅', '⠃', '⡃', '⠇', '⡇',
          '⢀', '⣀', '⢄', '⣄', '⢂', '⣂', '⢆', '⣆', '⢁', '⣁', '⢅', '⣅', '⢃', '⣃', '⢇', '⣇',
          '⠠', '⡠', '⠤', '⡤', '⠢', '⡢', '⠦', '⡦', '⠡', '⡡', '⠥', '⡥', '⠣', '⡣', '⠧', '⡧',
          '⢠', '⣠', '⢤', '⣤', '⢢', '⣢', '⢦', '⣦', '⢡', '⣡', '⢥', '⣥', '⢣', '⣣', '⢧', '⣧',
          '⠐', '⡐', '⠔', '⡔', '⠒', '⡒', '⠖', '⡖', '⠑', '⡑', '⠕', '⡕', '⠓', '⡓', '⠗', '⡗',
          '⢐', '⣐', '⢔', '⣔', '⢒', '⣒', '⢖', '⣖', '⢑', '⣑', '⢕', '⣕', '⢓', '⣓', '⢗', '⣗',
          '⠰', '⡰', '⠴', '⡴', '⠲', '⡲', '⠶', '⡶', '⠱', '⡱', '⠵', '⡵', '⠳', '⡳', '⠷', '⡷',
          '⢰', '⣰', '⢴', '⣴', '⢲', '⣲', '⢶', '⣶', '⢱', '⣱', '⢵', '⣵', '⢳', '⣳', '⢷', '⣷',
          '⠈', '⡈', '⠌', '⡌', '⠊', '⡊', '⠎', '⡎', '⠉', '⡉', '⠍', '⡍', '⠋', '⡋', '⠏', '⡏',
          '⢈', '⣈', '⢌', '⣌', '⢊', '⣊', '⢎', '⣎', '⢉', '⣉', '⢍', '⣍', '⢋', '⣋', '⢏', '⣏',
          '⠨', '⡨', '⠬', '⡬', '⠪', '⡪', '⠮', '⡮', '⠩', '⡩', '⠭', '⡭', '⠫', '⡫', '⠯', '⡯',
          '⢨', '⣨', '⢬', '⣬', '⢪', '⣪', '⢮', '⣮', '⢩', '⣩', '⢭', '⣭', '⢫', '⣫', '⢯', '⣯',
          '⠘', '⡘', '⠜', '⡜', '⠚', '⡚', '⠞', '⡞', '⠙', '⡙', '⠝', '⡝', '⠛', '⡛', '⠟', '⡟',
          '⢘', '⣘', '⢜', '⣜', '⢚', '⣚', '⢞', '⣞', '⢙', '⣙', '⢝', '⣝', '⢛', '⣛', '⢟', '⣟',
          '⠸', '⡸', '⠼', '⡼', '⠺', '⡺', '⠾', '⡾', '⠹', '⡹', '⠽', '⡽', '⠻', '⡻', '⠿', '⡿',
          '⢸', '⣸', '⢼', '⣼', '⢺', '⣺', '⢾', '⣾', '⢹', '⣹', '⢽', '⣽', '⢻', '⣻', '⢿', '⣿'
        });

        private final int nr;
        private final int nc;
        private final char left;
        private final char right;
        private final String tip;
        private final char[] symbols;

        PrintProbeUnicodeSymbols(int nr, int nc, char left, char right, String tip, char[] symbols) {
            this.nr = nr;
            this.nc = nc;
            this.left = left;
            this.right = right;
            this.tip = tip;
            this.symbols = symbols;
        }

        public static PrintProbeUnicodeSymbols getInstance() {
            var prop = System.getProperty("io.ast.jneurocarto.probe_npx.print_probe_symbol", "22");
            return switch (prop) {
                case "42" -> S42;
                default -> S22;
            };
        }

        public boolean isBlank(char c) {
            return c == symbols[0] || c == left || c == right;
        }

        public boolean isBlank(String row) {
            for (int i = 1, len = row.length(); i < len; i++) {
                var c = row.charAt(i);
                if (!isBlank(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class PrintProbeHelper {
        int ns;
        int nr;
        int nc;
        PrintProbeUnicodeSymbols pus;

        PrintProbeHelper(NpxProbeType type, PrintProbeUnicodeSymbols pus) {
            this.pus = pus;
            ns = type.nShank();
            nr = type.nRowPerShank() / pus.nr;
            nc = type.nColumnPerShank() / pus.nc;
        }

        int[] newArray() {
            return new int[ns * nr * nc];
        }

        void setCodeAt(int[] arr, Electrode e) {
            var s = e.shank;
            var ci = e.column / pus.nc;
            var cj = e.column % pus.nc;
            var ri = e.row / pus.nr;
            var rj = e.row % pus.nr;
            var i = indexOf(s, ri, ci);
            arr[i] = arr[i] | (1 << (pus.nr * cj)) * (1 << rj);
        }

        int indexOf(int s, int r, int c) {
            // (R, S, C)
            return r * ns * nc + s * nc + c;
        }
    }

    private static List<String> printProbeRaw(ChannelMap chmap, PrintProbeUnicodeSymbols pus) {
        var helper = new PrintProbeHelper(chmap.type(), pus);

        var arr = helper.newArray();
        for (var e : chmap) {
            if (e != null) {
                helper.setCodeAt(arr, e);
            }
        }

        var ret = new ArrayList<String>(helper.nr);
        var tmp = new char[helper.ns * (helper.nc + 2)];

        for (int r = 0; r < helper.nr; r++) {
            int k = 0;

            for (int s = 0; s < helper.ns; s++) {
                tmp[k++] = pus.left;
                for (int c = 0; c < helper.nc; c++) {
                    tmp[k++] = pus.symbols[arr[helper.indexOf(s, r, c)]];
                }
                tmp[k++] = pus.right;
            }
            ret.add(new String(tmp));
        }

        return ret;
    }
}
