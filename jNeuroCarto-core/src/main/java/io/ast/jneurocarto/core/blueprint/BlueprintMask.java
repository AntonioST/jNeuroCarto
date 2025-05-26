package io.ast.jneurocarto.core.blueprint;

import java.util.BitSet;
import java.util.function.IntPredicate;

public record BlueprintMask(int length, BitSet mask) implements IntPredicate {
    public BlueprintMask(int length) {
        this(length, new BitSet(length));
    }

    public BlueprintMask(boolean[] mask) {
        this(mask.length, new BitSet(mask.length));
        for (int i = 0, length = mask.length; i < length; i++) {
            if (mask[i]) this.mask.set(i);
        }
    }

    public BlueprintMask(BlueprintMask mask) {
        this(mask.length);
        this.mask.or(mask.mask);
    }

    public int count() {
        return mask.cardinality();
    }

    @Override
    public boolean test(int value) {
        return mask.get(value);
    }

    public void set(int i) {
        mask.set(i);
    }

    public void set(int i, boolean value) {
        mask.set(i, value);
    }

    /**
     * {@code this[mask] = other[mask]}
     *
     * @param other
     * @param mask
     */
    public void set(BlueprintMask other, BlueprintMask mask) {
        if (length != other.length) throw new RuntimeException();
        if (length != mask.length) throw new RuntimeException();
        // (this | mask) & (other & mask)
        this.mask.or(mask.mask);
        this.mask.and(other.and(mask).mask);
    }

    /**
     * {@code this[writeMask] = other[readMask]}
     *
     * @param writeMask
     * @param other
     * @param readMask
     */
    public void set(BlueprintMask writeMask, BlueprintMask other, BlueprintMask readMask) {
        if (length != other.length) throw new RuntimeException();
        if (length != writeMask.length) throw new RuntimeException();
        if (length != readMask.length) throw new RuntimeException();
        if (writeMask.count() != readMask.count()) throw new RuntimeException();

        var i = writeMask.mask.nextSetBit(0);
        var j = readMask.mask.nextSetBit(0);
        while (i >= 0) {
            writeMask.set(i, other.mask.get(j));
            i = writeMask.mask.nextSetBit(i + 1);
            j = readMask.mask.nextSetBit(j + 1);
        }
    }

    public BlueprintMask not() {
        var ret = new BlueprintMask(this);
        ret.mask.flip(0, length);
        return ret;
    }

    public BlueprintMask and(BlueprintMask other) {
        if (length != other.length) throw new RuntimeException();
        var ret = new BlueprintMask(this);
        ret.mask.and(other.mask);
        return ret;
    }

    public BlueprintMask or(BlueprintMask other) {
        if (length != other.length) throw new RuntimeException();
        var ret = new BlueprintMask(this);
        ret.mask.or(other.mask);
        return ret;
    }

    public BlueprintMask xor(BlueprintMask other) {
        if (length != other.length) throw new RuntimeException();
        var ret = new BlueprintMask(this);
        ret.mask.xor(other.mask);
        return ret;
    }

    public int previousClearBit(int fromIndex) {
        return mask.previousClearBit(fromIndex);
    }

    public int previousSetBit(int fromIndex) {
        return mask.previousSetBit(fromIndex);
    }

    public int nextClearBit(int fromIndex) {
        return mask.nextClearBit(fromIndex);
    }

    public int nextSetBit(int fromIndex) {
        return mask.nextSetBit(fromIndex);
    }

    public int[] asElectrodeIndex() {
        return mask.stream().toArray();
    }

    public boolean[] asBooleanMask() {
        var ret = new boolean[length];
        mask.stream().forEach(i -> ret[i] = true);
        return ret;
    }
}
