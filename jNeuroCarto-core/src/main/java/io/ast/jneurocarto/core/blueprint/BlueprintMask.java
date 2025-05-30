package io.ast.jneurocarto.core.blueprint;

import java.util.Arrays;
import java.util.BitSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.*;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

public record BlueprintMask(int length, BitSet mask) implements IntPredicate {
    public BlueprintMask(int length) {
        this(length, new BitSet(length));
    }

    public BlueprintMask(BlueprintMask mask) {
        this(mask.length);
        this.mask.or(mask.mask);
    }

    public int count() {
        return mask.cardinality();
    }

    public boolean any() {
        return mask.nextSetBit(0) >= 0;
    }

    public boolean all() {
        return mask.nextSetBit(0) < 0;
    }

    @Override
    public boolean test(int i) {
        return mask.get(i);
    }

    public boolean get(int i) {
        return mask.get(i);
    }

    /**
     * Get the index of the n-th set bit.
     *
     * @param n n-th
     * @return position index. {@code -1} if {@code n} over {@link #count()}
     */
    public int getSet(int n) {
        if (n < 0) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
            if (j == n) return i;
        }
        return -1;
    }

    /**
     * Get the index of the n-th clear bit.
     *
     * @param n n-th
     * @return position index. {@code -1} if {@code n} over {@link #count()}
     */
    public int getClear(int n) {
        if (n < 0) throw new IllegalArgumentException();
        for (int i = nextClearIndex(0), j = 0; i >= 0; i = nextClearIndex(i + 1), j++) {
            if (j == n) return i;
        }
        return -1;
    }

    public void set(int i) {
        if (!(0 <= i && i < length)) throw new IllegalArgumentException();
        mask.set(i);
    }

    public void set(int i, boolean value) {
        if (!(0 <= i && i < length)) throw new IllegalArgumentException();
        mask.set(i, value);
    }

    /**
     * {@code this[mask] = other[mask]}
     *
     * @param other
     * @param mask
     */
    public void set(BlueprintMask other, BlueprintMask mask) {
        if (length != other.length) throw new IllegalArgumentException();
        if (length != mask.length) throw new IllegalArgumentException();
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
        if (length != other.length) throw new IllegalArgumentException();
        if (length != writeMask.length) throw new IllegalArgumentException();
        if (length != readMask.length) throw new IllegalArgumentException();
        if (writeMask.count() != readMask.count()) throw new IllegalArgumentException();

        if (this == other) {
            other = new BlueprintMask(other);
        }

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
        if (length != other.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(this);
        if (this == other) return ret;
        ret.mask.and(other.mask);
        return ret;
    }

    public BlueprintMask or(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(this);
        if (this == other) return ret;
        ret.mask.or(other.mask);
        return ret;
    }

    public BlueprintMask xor(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        if (this == other) return new BlueprintMask(length);
        var ret = new BlueprintMask(this);
        ret.mask.xor(other.mask);
        return ret;
    }

    public BlueprintMask diff(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        if (this == other) return new BlueprintMask(length);
        var ret = new BlueprintMask(this);
        ret.mask.and(other.mask);
        ret.mask.flip(0, length);
        ret.mask.and(this.mask);
        return ret;
    }

    public int previousClearIndex(int fromIndex) {
        return mask.previousClearBit(fromIndex);
    }

    public int previousSetIndex(int fromIndex) {
        return mask.previousSetBit(fromIndex);
    }

    public int nextClearIndex(int fromIndex) {
        return mask.nextClearBit(fromIndex);
    }

    public int nextSetIndex(int fromIndex) {
        return mask.nextSetBit(fromIndex);
    }

    public IntStream stream() {
        return mask.stream();
    }

    public void forEach(IntConsumer set) {
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            set.accept(i);
        }
    }

    public void forEachClear(IntConsumer set) {
        for (int i = nextClearIndex(0); i >= 0; i = nextClearIndex(i + 1)) {
            set.accept(i);
        }
    }

    public int[] asElectrodeIndex() {
        return mask.stream().toArray();
    }

    public static BlueprintMask fromElectrodeIndex(int total, int[] index) {
        var ret = new BlueprintMask(total);
        for (int e : index) {
            ret.set(e);
        }
        return ret;
    }

    public static BlueprintMask fromElectrodeIndex(int total, int[] index, int offset, int length) {
        var ret = new BlueprintMask(total);
        for (int i = 0; i < length; i++) {
            ret.set(index[i + offset]);
        }
        return ret;
    }

    public boolean[] asBooleanMask() {
        var ret = new boolean[length];
        mask.stream().forEach(i -> ret[i] = true);
        return ret;
    }

    public static BlueprintMask fromBooleanMask(boolean[] mask) {
        var ret = new BlueprintMask(mask.length);
        for (int i = 0, length = mask.length; i < length; i++) {
            if (mask[i]) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask all(int length) {
        var ret = new BlueprintMask(length);
        ret.mask.flip(0, length);
        return ret;
    }

    public static BlueprintMask all(int[] array) {
        return all(array.length);
    }

    public static BlueprintMask all(double[] array) {
        return all(array.length);
    }

    public static BlueprintMask eq(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] == value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask ne(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] != value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask lt(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] < value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask le(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] <= value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask gt(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] > value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask ge(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] >= value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask eq(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] == value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask ne(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] != value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask lt(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] < value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask le(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] <= value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask gt(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] > value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask ge(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] >= value) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask nan(double[] array) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (Double.isNaN(array[i])) ret.set(i);
        }
        return ret;
    }

    public static BlueprintMask notNan(double[] array) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (!Double.isNaN(array[i])) ret.set(i);
        }
        return ret;
    }

    /**
     * {@code array[this] = value}
     *
     * @param array output array
     * @param value fill value
     */
    public void fill(int[] array, int value) {
        if (length != array.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            array[i] = value;
        }
    }

    /**
     * {@code array[this] = value}
     *
     * @param array output array
     * @param value fill value
     */
    public void fill(double[] array, double value) {
        if (length != array.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            array[i] = value;
        }
    }

    /**
     * {@code return array[this]}
     *
     * @param array source array
     */
    public int[] squeeze(int[] array) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = new int[count()];
        for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
            ret[j] = array[i];
        }
        return ret;
    }

    /**
     * {@code output[:count()] = array[this]}
     *
     * @param array source array
     * @return {@link #count()}
     */
    public int squeeze(int[] output, int[] array) {
        if (length != array.length) throw new IllegalArgumentException();
        var count = count();
        if (output.length < count) throw new IllegalArgumentException();
        if (output == array) {
            for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
                if (j < i) output[j] = array[i];
            }
        } else {
            for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
                output[j] = array[i];
            }
        }
        return count;
    }

    /**
     * {@code return array[this]}
     *
     * @param array source array
     */
    public double[] squeeze(double[] array) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = new double[count()];
        for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
            ret[j] = array[i];
        }
        return ret;
    }

    /**
     * {@code output[:count()] = array[this]}
     *
     * @param array source array
     * @return {@link #count()}
     */
    public int squeeze(double[] output, double[] array) {
        if (length != array.length) throw new IllegalArgumentException();
        var count = count();
        if (output.length < count) throw new IllegalArgumentException();
        if (output == array) {
            for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
                if (j < i) output[j] = array[i];
            }
        } else {
            for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
                output[j] = array[i];
            }
        }
        return count;
    }

    /**
     * {@code np.where(this, array, otherwise)}
     * <p/>
     * where {@code return[this] == array[this]} and {@code return[!this] == otherwise}.
     *
     * @param array     source array
     * @param otherwise false value
     * @return
     */
    public int[] where(int[] array, int otherwise) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = new int[length];
        Arrays.fill(ret, otherwise);
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret[i] = array[i];
        }
        return ret;
    }

    /**
     * {@code output[:] = np.where(this, array, output)}
     * <p/>
     * where {@code output[this] == array[this]} and {@code output[!this]} kept unchange.
     *
     * @param output output array
     * @param array  source array
     */
    public void where(int[] output, int[] array) {
        if (length != output.length) throw new IllegalArgumentException();
        if (length != array.length) throw new IllegalArgumentException();
        if (output != array) {
            for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
                output[i] = array[i];
            }
        }
    }

    /**
     * {@code output[:] = np.where(this, array, otherwise)}
     * <p/>
     * where {@code output[this] == array[this]} and {@code output[!this] == otherwise}
     *
     * @param output    output array
     * @param array     source array
     * @param otherwise false value
     */
    public void where(int[] output, int[] array, int otherwise) {
        if (length != output.length) throw new IllegalArgumentException();
        if (length != array.length) throw new IllegalArgumentException();
        if (output == array) throw new RuntimeException("undefined behavior when output and array are the same object.");
        Arrays.fill(output, otherwise);
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            output[i] = array[i];
        }
    }

    /**
     * {@code output[this] = array[mask]}
     * <p/>
     * If {@code mask} is {@code null}, then {@code output[this] = array[:]}.
     *
     * @param output output array
     * @param array  source array
     * @param mask   mask on array
     */
    public void where(int[] output, int[] array, @Nullable BlueprintMask mask) {
        if (this.length != output.length) throw new IllegalArgumentException();
        if (mask == null) {
            if (count() != array.length) throw new IllegalArgumentException();
            if (output == array) throw new RuntimeException("undefined behavior when output and array are the same object.");
            for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
                output[i] = array[j];
            }
        } else {
            if (mask.length != array.length) throw new IllegalArgumentException();
            if (this.count() != mask.count()) throw new IllegalArgumentException();
            if (output == array && this.and(mask).any()) {
                throw new RuntimeException("undefined behavior when output and array are the same object and masks are overlapped");
            }
            int i = this.nextSetIndex(0);
            int j = mask.nextSetIndex(0);
            while (i >= 0) {
                output[i] = array[j];
                i = this.nextSetIndex(i + 1);
                j = mask.nextSetIndex(j + 1);
            }
        }
    }

    /**
     * {@code np.where(this, array, otherwise)}
     * <p/>
     * where {@code return[this] == array[this]} and {@code return[!this] == otherwise}.
     *
     * @param array     source array
     * @param otherwise false value
     * @return
     */
    public double[] where(double[] array, double otherwise) {
        if (length != array.length) throw new RuntimeException();
        var ret = new double[length];
        Arrays.fill(ret, otherwise);
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret[i] = array[i];
        }
        return ret;
    }

    /**
     * {@code output[:] = np.where(this, array, output)}
     * <p/>
     * where {@code output[this] == array[this]} and {@code output[!this]} kept unchange.
     *
     * @param output output array
     * @param array  source array
     */
    public void where(double[] output, double[] array) {
        if (length != output.length) throw new IllegalArgumentException();
        if (length != array.length) throw new IllegalArgumentException();
        if (output != array) {
            for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
                output[i] = array[i];
            }
        }
    }

    /**
     * {@code output[:] = np.where(this, array, otherwise)}
     * <p/>
     * where {@code output[this] == array[this]} and {@code output[!this] == otherwise}
     *
     * @param output    output array
     * @param array     source array
     * @param otherwise false value
     */
    public void where(double[] output, double[] array, double otherwise) {
        if (length != output.length) throw new IllegalArgumentException();
        if (length != array.length) throw new IllegalArgumentException();
        if (output == array) throw new RuntimeException("undefined behavior when output and array are the same object.");
        Arrays.fill(output, otherwise);
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            output[i] = array[i];
        }
    }

    /**
     * {@code output[this] = array[mask]}.
     * <p/>
     * If {@code mask} is {@code null}, then {@code output[this] = array[:]}.
     *
     * @param output output array
     * @param array  source array
     * @param mask   mask on array
     */
    public void where(double[] output, double[] array, @Nullable BlueprintMask mask) {
        if (this.length != output.length) throw new IllegalArgumentException();
        if (mask == null) {
            if (count() != array.length) throw new IllegalArgumentException();
            if (output == array) throw new RuntimeException("undefined behavior when output and array are the same object.");
            for (int i = nextSetIndex(0), j = 0; i >= 0; i = nextSetIndex(i + 1), j++) {
                output[i] = array[j];
            }
        } else {
            if (mask.length != array.length) throw new IllegalArgumentException();
            if (this.count() != mask.count()) throw new IllegalArgumentException();
            if (output == array && this.and(mask).any()) {
                throw new IllegalArgumentException("undefined behavior when output and array are the same object and masks are overlapped");
            }
            int i = this.nextSetIndex(0);
            int j = mask.nextSetIndex(0);
            while (i >= 0) {
                output[i] = array[j];
                i = this.nextSetIndex(i + 1);
                j = mask.nextSetIndex(j + 1);
            }
        }
    }

    public int[] map(int[] array, IntUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = new int[length];
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret[i] = oper.applyAsInt(array[i]);
        }
        return ret;
    }

    public void map(int[] output, int[] array, IntUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        if (length != output.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            output[i] = oper.applyAsInt(array[i]);
        }
    }

    public double[] map(double[] array, DoubleUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = new double[length];
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret[i] = oper.applyAsDouble(array[i]);
        }
        return ret;
    }

    public void map(double[] output, double[] array, DoubleUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        if (length != output.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            output[i] = oper.applyAsDouble(array[i]);
        }
    }

    /**
     * {@code reduce(oper, array[this])}
     *
     * @param array source array
     * @param oper  operator
     * @return
     */
    public OptionalInt fold(int[] array, IntBinaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = 0;
        var init = false;
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            if (init) {
                ret = oper.applyAsInt(ret, array[i]);
            } else {
                ret = array[i];
                init = true;
            }
        }
        return init ? OptionalInt.of(ret) : OptionalInt.empty();
    }

    /**
     * {@code reduce(oper, init, array[this])}
     *
     * @param array source array
     * @param init  initial value
     * @param oper  operator
     * @return
     */
    public int fold(int[] array, int init, IntBinaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = init;
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret = oper.applyAsInt(ret, array[i]);
        }
        return ret;
    }

    /**
     * {@code reduce(oper, array[this])}
     *
     * @param array source array
     * @param oper  operator
     * @return
     */
    public OptionalDouble fold(double[] array, DoubleBinaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = 0.0;
        var init = false;
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            if (init) {
                ret = oper.applyAsDouble(ret, array[i]);
            } else {
                ret = array[i];
                init = true;
            }
        }
        return init ? OptionalDouble.of(ret) : OptionalDouble.empty();
    }

    /**
     * {@code reduce(oper, init, array[this])}
     *
     * @param array source array
     * @param init  initial value
     * @param oper  operator
     * @return
     */
    public double fold(double[] array, double init, DoubleBinaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = init;
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret = oper.applyAsDouble(ret, array[i]);
        }
        return ret;
    }
}
