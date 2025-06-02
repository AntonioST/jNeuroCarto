package io.ast.jneurocarto.javafx.chart.colormap;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

import io.ast.jneurocarto.core.blueprint.MinMax;

public record Normalize(double lower, double upper) implements DoubleUnaryOperator {

    public static final Normalize N01 = new Normalize(0, 1);

    public Normalize {
        if (!(lower <= upper)) throw new IllegalArgumentException();
    }

    public Normalize(MinMax minmax) {
        this(minmax.min(), minmax.max());
    }

    public static Normalize union(Normalize[] normalizes) {
        return union(Arrays.asList(normalizes));
    }

    public static Normalize union(Normalize[] normalizes, Normalize init) {
        return union(Arrays.asList(normalizes), init);
    }

    public static Normalize union(List<Normalize> normalizes) {
        if (normalizes.isEmpty()) throw new RuntimeException();
        if (normalizes.size() == 1) return normalizes.get(0);

        var result = normalizes.stream()
          .map(n -> new MinMax(n.lower, n.upper))
          .gather(MinMax.minmax())
          .findFirst()
          .get();

        return new Normalize(result);
    }

    public static Normalize union(List<Normalize> normalizes, Normalize init) {
        if (normalizes.isEmpty()) return init;

        var stream = Stream.concat(Stream.of(init), normalizes.stream());
        var result = stream.map(n -> new MinMax(n.lower, n.upper))
          .gather(MinMax.minmax())
          .findFirst()
          .get();

        return new Normalize(result);
    }

    public static Gatherer<Normalize, ?, Normalize> union() {
        var toMM = Gatherer.<Normalize, MinMax>of((_, e, d) -> d.push(new MinMax(e.lower, e.upper)));
        var toNM = Gatherer.<MinMax, Normalize>of((_, e, d) -> d.push(new Normalize(e)));
        return toMM.andThen(MinMax.minmax()).andThen(toNM);
    }

    @Override
    public double applyAsDouble(double operand) {
        if (operand <= lower) return 0;
        if (operand >= upper) return 1;
        return (operand - lower) / (upper - lower);
    }
}
