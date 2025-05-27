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
     * var result = stream.boxed().gather(MinMaxInt.minmax()).findFirst().get();
     *}
     *
     * @return
     */
    public static Gatherer<Integer, ?, MinMaxInt> minmax() {
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
          (state, downstream) -> downstream.push(state[0])
        );
    }
}
