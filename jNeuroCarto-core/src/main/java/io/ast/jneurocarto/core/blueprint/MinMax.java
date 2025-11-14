package io.ast.jneurocarto.core.blueprint;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Gatherer;

public sealed interface MinMax<T> {

    MinMax<T> consume(MinMax<T> other);

    record OfInt(int min, int max) implements MinMax<Integer> {

        public OfInt {
            if (min > max) {
                throw new IllegalArgumentException();
            }
        }

        public OfInt(int value) {
            this(value, value);
        }

        @Override
        public OfInt consume(MinMax<Integer> other) {
            if (other instanceof OfInt(int min1, int max1)) {
                var min = Math.min(this.min, min1);
                var max = Math.max(this.max, max1);
                return new OfInt(min, max);
            } else {
                throw new ClassCastException();
            }
        }

        public long range() {
            return (long) max - min;
        }
    }

    record OfDouble(double min, double max) implements MinMax<Double> {
        public OfDouble {
            var nanMin = Double.isNaN(min);
            var nanMax = Double.isNaN(max);
            if (nanMin && nanMax) {
                //
            } else if (nanMin || nanMax) {
                throw new IllegalArgumentException("one of min or max is NaN");
            } else if (min > max) {
                throw new IllegalArgumentException("min > max");
            }
        }

        public OfDouble(double value) {
            this(value, value);
        }

        public OfDouble consume(MinMax<Double> other) {
            if (other instanceof OfDouble that) {
                if (that.isNaN()) return this;
                if (isNaN()) return that;

                var min = Math.min(this.min, that.min);
                var max = Math.max(this.max, that.max);
                return new OfDouble(min, max);
            } else {
                throw new ClassCastException();
            }
        }

        public double range() {
            return max - min;
        }

        public boolean isNaN() {
            return Double.isNaN(min);
        }
    }

    record OfRef<T>(T min, T max) implements MinMax<T> {
        public OfRef(T value) {
            this(value, value);
        }

        public OfRef<T> consume(MinMax<T> other) {
            throw new RuntimeException("use consume(MinMax, Comparator) instead");
        }

        public OfRef<T> consume(MinMax<T> other, Comparator<? super T> cmp) {
            if (other instanceof OfRef(T min1, T max1)) {
                var min = cmp.compare(this.min, min1) < 0 ? this.min : min1;
                var max = cmp.compare(this.max, max1) > 0 ? this.max : max1;
                return new OfRef<>(min, max);
            } else {
                throw new ClassCastException();
            }
        }
    }


    /**
     * {@snippet lang = "java":
     * import java.util.stream.IntStream;
     * IntStream stream = IntStream.of(); // @replace regex="IntStream\.of\(\)" replacement="..."
     * var result = stream.boxed().gather(MinMax.intMinmax()).findFirst().get();
     *}
     *
     * @return
     */
    static Gatherer<Integer, ?, OfInt> intMinmax() {
        return Gatherer.of(
          () -> new OfInt[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              var minmax = new OfInt(element);
              if (state[0] == null) {
                  state[0] = minmax;
              } else {
                  state[0] = state[0].consume(minmax);
              }
              return true;
          }),
          (state, other) -> {
              state[0] = state[0].consume(other[0]);
              return state;
          },
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

    /**
     * {@snippet lang = "java":
     * import java.util.stream.DoubleStream;
     * DoubleStream stream = DoubleStream.of(); // @replace regex="DoubleStream\.of\(\)" replacement="..."
     * var result = stream.boxed().gather(MinMax.doubleMinmax()).findFirst().get();
     *}
     *
     * @return
     */
    static Gatherer<Double, ?, OfDouble> doubleMinmax() {
        return Gatherer.of(
          () -> new OfDouble[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              if (!Double.isNaN(element)) {
                  var minmax = new OfDouble(element);
                  if (state[0] == null) {
                      state[0] = minmax;
                  } else {
                      state[0] = state[0].consume(minmax);
                  }
              }
              return true;
          }),
          (state, other) -> {
              state[0] = state[0].consume(other[0]);
              return state;
          },
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

    static <T> Gatherer<T, ?, OfRef<T>> minmax(Comparator<? super T> cmp) {
        Objects.requireNonNull(cmp, "comparator");

        return Gatherer.of(
          () -> new OfRef[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              var minmax = new OfRef<T>(element);
              if (state[0] == null) {
                  state[0] = minmax;
              } else {
                  state[0] = state[0].consume(minmax, cmp);
              }
              return true;
          }),
          (state, other) -> {
              state[0] = state[0].consume(other[0], cmp);
              return state;
          },
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

    static <T> Gatherer<T, ?, OfInt> intMinmax(Function<T, MinMax.OfInt> mapper) {
        Objects.requireNonNull(mapper);

        return Gatherer.of(
          () -> new OfInt[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              var minmax = mapper.apply(element);
              if (state[0] == null) {
                  state[0] = minmax;
              } else {
                  state[0] = state[0].consume(minmax);
              }
              return true;
          }),
          (state, other) -> {
              state[0] = state[0].consume(other[0]);
              return state;
          },
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

    static <T> Gatherer<T, ?, OfDouble> doubleMinmax(Function<T, MinMax.OfDouble> mapper) {
        Objects.requireNonNull(mapper);

        return Gatherer.of(
          () -> new OfDouble[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              var minmax = mapper.apply(element);
              if (state[0] == null) {
                  state[0] = minmax;
              } else {
                  state[0] = state[0].consume(minmax);
              }
              return true;
          }),
          (state, other) -> {
              state[0] = state[0].consume(other[0]);
              return state;
          },
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

    static <T extends Comparable<? super T>> Gatherer<T, ?, OfRef<T>> minmax(Function<T, MinMax.OfRef<T>> mapper) {
        Objects.requireNonNull(mapper);
        return minmax(mapper, Comparable::compareTo);
    }

    static <T> Gatherer<T, ?, OfRef<T>> minmax(Function<T, MinMax.OfRef<T>> mapper, Comparator<? super T> cmp) {
        Objects.requireNonNull(mapper);
        Objects.requireNonNull(cmp, "comparator");

        return Gatherer.of(
          () -> new OfRef[1],
          Gatherer.Integrator.ofGreedy((state, element, _) -> {
              var minmax = mapper.apply(element);
              if (state[0] == null) {
                  state[0] = minmax;
              } else {
                  state[0] = state[0].consume(minmax, cmp);
              }
              return true;
          }),
          (state, other) -> {
              state[0] = state[0].consume(other[0], cmp);
              return state;
          },
          (state, downstream) -> {
              if (state[0] != null) downstream.push(state[0]);
          }
        );
    }

}
