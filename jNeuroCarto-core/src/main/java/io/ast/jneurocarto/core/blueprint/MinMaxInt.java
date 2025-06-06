package io.ast.jneurocarto.core.blueprint;

import java.util.stream.Gatherer;

public record MinMaxInt(int min, int max) {
    public MinMaxInt(int value) {
        this(value, value);
    }

    public MinMaxInt consume(MinMaxInt other) {
        return new MinMaxInt(Math.min(min, other.min), Math.max(max, other.max));
    }

    public int range() {
        return max - min;
    }

    /**
     * {@snippet lang = "java":
     * import java.util.stream.IntStream;
     * IntStream stream = IntStream.of(); // @replace regex="IntStream\.of\(\)" replacement="..."
     * var result = stream.boxed().gather(MinMaxInt.intMinmax()).findFirst().get();
     *}
     *
     * @return
     */
    public static Gatherer<Integer, ?, MinMaxInt> intMinmax() {
        return Gatherer.ofSequential(
          () -> new MinMaxInt[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              var minmax = new MinMaxInt(element);
              if (state[0] == null) {
                  state[0] = minmax;
              } else {
                  state[0] = state[0].consume(minmax);
              }
              return true;
          }),
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

    public static Gatherer<MinMaxInt, ?, MinMaxInt> minmax() {
        return Gatherer.ofSequential(
          () -> new MinMaxInt[1],
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
