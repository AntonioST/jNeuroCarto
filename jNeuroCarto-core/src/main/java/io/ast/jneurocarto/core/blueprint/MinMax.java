package io.ast.jneurocarto.core.blueprint;

import java.util.stream.Gatherer;

public record MinMax(double min, double max) {
    public MinMax(double value) {
        this(value, value);
    }

    public MinMax consume(MinMax other) {
        return new MinMax(Math.min(min, other.min), Math.max(max, other.max));
    }

    public double range() {
        return max - min;
    }

    /**
     * {@snippet lang = "java":
     * import java.util.stream.DoubleStream;
     * DoubleStream stream = DoubleStream.of(); // @replace regex="DoubleStream\.of\(\)" replacement="..."
     * var result = stream.boxed().gather(MinMaxInt.minmax()).findFirst().get();
     *}
     *
     * @return
     */
    public static Gatherer<Double, ?, MinMax> minmax() {
        return Gatherer.ofSequential(
          () -> new MinMax[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              if (!Double.isNaN(element)) {
                  var minmax = new MinMax(element);
                  if (state[0] == null) {
                      state[0] = minmax;
                  } else {
                      state[0] = state[0].consume(minmax);
                  }
              }
              return true;
          }),
          (state, downstream) -> downstream.push(state[0])
        );
    }
}
