package io.ast.jneurocarto.core.blueprint;

import java.util.BitSet;
import java.util.function.IntPredicate;

public record BlueprintMask(int length, BitSet set) implements IntPredicate {
    public BlueprintMask(int length) {
        this(length, new BitSet(length));
    }

    public BlueprintMask(boolean[] mask) {
        this(mask.length, new BitSet(mask.length));
        for (int i = 0, length = mask.length; i < length; i++) {
            if (mask[i]) set.set(i);
        }
    }

    public BlueprintMask(BlueprintMask mask) {
        this(mask.length);
        set.or(mask.set);
    }

    public int count() {
        return set.cardinality();
    }

    @Override
    public boolean test(int value) {
        return set.get(value);
    }

    public void set(int i) {
        set.set(i);
    }

    public void set(int i, boolean value) {
        set.set(i, value);
    }

    public BlueprintMask not() {
        var ret = new BlueprintMask(this);
        ret.set.flip(0, length);
        return ret;
    }

    public BlueprintMask and(BlueprintMask other) {
        if (length != other.length) throw new RuntimeException();
        var ret = new BlueprintMask(this);
        ret.set.and(other.set);
        return ret;
    }

    public BlueprintMask or(BlueprintMask other) {
        if (length != other.length) throw new RuntimeException();
        var ret = new BlueprintMask(this);
        ret.set.or(other.set);
        return ret;
    }

    public BlueprintMask xor(BlueprintMask other) {
        if (length != other.length) throw new RuntimeException();
        var ret = new BlueprintMask(this);
        ret.set.xor(other.set);
        return ret;
    }

    public int[] asElectrodeIndex() {
        return set.stream().toArray();
    }

    public boolean[] asBooleanMask() {
        var ret = new boolean[length];
        set.stream().forEach(i -> ret[i] = true);
        return ret;
    }
}
