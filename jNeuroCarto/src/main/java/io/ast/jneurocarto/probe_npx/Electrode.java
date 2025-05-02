package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record Electrode(
  int shank,
  int column,
  int row,
  boolean inUsed,
  int apBandGain,
  int lfBandBain,
  boolean apHpFilter
) implements Comparable<Electrode> {

    @Override
    public int hashCode() {
        int result = shank;
        result = 31 * result + column;
        result = 31 * result + row;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Electrode e) && shank == e.shank && column == e.column && row == e.row;
    }

    @Override
    public int compareTo(Electrode o) {
        if (shank != o.shank) {
            return shank < o.shank ? -1 : 1;
        } else if (row != o.row) {
            return row < o.row ? -1 : 1;
        } else if (column != o.column) {
            return column < o.column ? -1 : 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "Electrode[" + shank + "," + column + "," + row + "]";
    }
}
