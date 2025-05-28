package io.ast.jneurocarto.core.blueprint;

import java.util.stream.Gatherer;

public record MinMax(double min, double max) {
    public MinMax(double value) {
        this(value, value);
    }

    public MinMax consume(MinMax other) {
        var min = Math.min(this.min, other.min);
        var max = Math.max(this.max, other.max);
        if (Double.isNaN(min)) {
            min = !Double.isNaN(this.min) ? this.min : other.min;
        }
        if (Double.isNaN(max)) {
            max = !Double.isNaN(this.max) ? this.max : other.max;
        }
        return new MinMax(min, max);
    }

    public double range() {
        return max - min;
    }

    /**
     * {@snippet lang = "java":
     * import java.util.stream.DoubleStream;
     * DoubleStream stream = DoubleStream.of(); // @replace regex="DoubleStream\.of\(\)" replacement="..."
     * var result = stream.boxed().gather(MinMaxInt.intMinmax()).findFirst().get();
     *}
     *
     * @return
     */
    public static Gatherer<Double, ?, MinMax> doubleMinmax() {
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
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

    public static Gatherer<MinMax, ?, MinMax> minmax() {
        return Gatherer.ofSequential(
          () -> new MinMax[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              if (state[0] == null) {
                  state[0] = element;
              } else {
                  state[0] = state[0].consume(element);
              }
              return true;
          }),
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }
}
