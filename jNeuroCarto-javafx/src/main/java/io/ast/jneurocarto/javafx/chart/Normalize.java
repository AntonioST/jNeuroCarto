package io.ast.jneurocarto.javafx.chart;

import java.util.function.DoubleUnaryOperator;

public record Normalize(double lower, double upper) implements DoubleUnaryOperator {

    public static final Normalize N01 = new Normalize(0, 1);

    public Normalize {
        if (!(lower <= upper)) throw new IllegalArgumentException();
    }

    @Override
    public double applyAsDouble(double operand) {
        if (operand <= lower) return 0;
        if (operand >= upper) return 1;
        return (operand - lower) / (upper - lower);
    }
}
