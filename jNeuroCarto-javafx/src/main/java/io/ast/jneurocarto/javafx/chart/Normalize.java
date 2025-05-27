package io.ast.jneurocarto.javafx.chart;

import java.util.function.DoubleUnaryOperator;

import io.ast.jneurocarto.core.blueprint.MinMax;

public record Normalize(double lower, double upper) implements DoubleUnaryOperator {

    public static final Normalize N01 = new Normalize(0, 1);

    public Normalize {
        if (!(lower <= upper)) throw new IllegalArgumentException();
    }

    public Normalize(MinMax minmax) {
        this(minmax.min(), minmax.max());
    }

    @Override
    public double applyAsDouble(double operand) {
        if (operand <= lower) return 0;
        if (operand >= upper) return 1;
        return (operand - lower) / (upper - lower);
    }
}
