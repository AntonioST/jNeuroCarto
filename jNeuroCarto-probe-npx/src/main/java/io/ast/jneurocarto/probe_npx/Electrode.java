package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Electrode implements Comparable<Electrode> {
    public final int shank;
    public final int column;
    public final int row;

    public boolean inUsed;
    public int apBandGain = 0;
    public int lfBandBain = 0;
    public boolean apHpFilter = false;

    public Electrode(int shank, int column, int row) {
        if (shank < 0) throw new IllegalArgumentException("negative shank value : " + shank);
        if (column < 0) throw new IllegalArgumentException("negative column value : " + column);
        if (row < 0) throw new IllegalArgumentException("negative row value : " + row);

        this.shank = shank;
        this.column = column;
        this.row = row;
    }

    public Electrode(Electrode ref) {
        this(ref.shank, ref.column, ref.row);
        copyFrom(ref);
    }

    public void copyFrom(Electrode ref) {
        apBandGain = ref.apBandGain;
        lfBandBain = ref.lfBandBain;
        apHpFilter = ref.apHpFilter;
    }

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
