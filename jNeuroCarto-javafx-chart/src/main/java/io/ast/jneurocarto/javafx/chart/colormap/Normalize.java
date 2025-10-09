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

    public Normalize(MinMax.OfInt minmax) {
        this(minmax.min(), minmax.max());
    }

    public Normalize(MinMax.OfDouble minmax) {
        this(minmax.min(), minmax.max());
    }

    private MinMax.OfDouble asMinmax() {
        return new MinMax.OfDouble(lower, upper);
    }


    public static Normalize union(Normalize[] normalizes) {
        return union(Arrays.asList(normalizes));
    }

    public static Normalize union(Normalize[] normalizes, Normalize init) {
        return union(Arrays.asList(normalizes), init);
    }

    public static Normalize union(List<Normalize> normalizes) {
        return normalizes.stream()
          .gather(MinMax.doubleMinmax(Normalize::asMinmax))
          .findFirst()
          .map(Normalize::new)
          .orElseThrow();
    }

    public static Normalize union(List<Normalize> normalizes, Normalize init) {
        return Stream.concat(Stream.of(init), normalizes.stream())
          .gather(MinMax.doubleMinmax(Normalize::asMinmax))
          .findFirst()
          .map(Normalize::new)
          .orElse(init);
    }

    public static Gatherer<Normalize, ?, Normalize> union() {
        var toNM = Gatherer.<MinMax.OfDouble, Normalize>of((_, e, d) -> d.push(new Normalize(e)));
        return MinMax.doubleMinmax(Normalize::asMinmax).andThen(toNM);
    }

    @Override
    public double applyAsDouble(double operand) {
        if (operand <= lower) return 0;
        if (operand >= upper) return 1;
        return (operand - lower) / (upper - lower);
    }
}
