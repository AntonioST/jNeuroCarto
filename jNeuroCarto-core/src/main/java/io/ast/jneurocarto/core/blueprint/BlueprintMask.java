package io.ast.jneurocarto.core.blueprint;

import java.util.Arrays;
import java.util.BitSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.*;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;

/**
 * blueprint mask.
 *
 * @param length total electrode number.
 * @param mask   internal mask data
 */
public record BlueprintMask(int length, BitSet mask) implements IntPredicate {

    /**
     * Create a mask with all false.
     *
     * @param length total electrode number.
     */
    public BlueprintMask(int length) {
        this(length, new BitSet(length));
    }

    /**
     * Clone the {@code mask}.
     *
     * @param mask another mask.
     */
    public BlueprintMask(BlueprintMask mask) {
        this(mask.length);
        this.mask.or(mask.mask);
    }

    /**
     * {@return number of set bits}
     */
    public int count() {
        return mask.cardinality();
    }

    /**
     * {@return any set bits}
     */
    public boolean any() {
        return mask.nextSetBit(0) >= 0;
    }

    /**
     * {@return all bits are set}
     */
    public boolean all() {
        return mask.nextClearBit(0) < 0;
    }

    /**
     * Get bit at given index.
     *
     * @param i the input argument
     * @return Is it set?
     * @throws IndexOutOfBoundsException {@code i} over {@link #length}
     */
    @Override
    public boolean test(int i) {
        return mask.get(i);
    }

    /**
     * Get bit at given index.
     *
     * @param i the input argument
     * @return Is it set?
     * @throws IndexOutOfBoundsException {@code i} over {@link #length}
     */
    public boolean get(int i) {
        return mask.get(i);
    }

    /**
     * Get the index of the n-th set bit.
     *
     * @param n n-th
     * @return position index. {@code -1} if {@code n} over {@link #count()}
     * @throws IllegalArgumentException {@code n} small then zero.
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
     * @throws IllegalArgumentException {@code n} small then zero.
     */
    public int getClear(int n) {
        if (n < 0) throw new IllegalArgumentException();
        for (int i = nextClearIndex(0), j = 0; i >= 0; i = nextClearIndex(i + 1), j++) {
            if (j == n) return i;
        }
        return -1;
    }

    /**
     * Set bit at {@code i} to {@code true}.
     *
     * @param i index.
     * @throws IndexOutOfBoundsException {@code i} over {@link #length}
     */
    public void set(int i) {
        if (!(0 <= i && i < length)) throw new IndexOutOfBoundsException();
        mask.set(i);
    }

    /**
     * Set bit at {@code i} to {@code value}.
     *
     * @param i     index.
     * @param value set or not
     * @throws IndexOutOfBoundsException {@code i} over {@link #length}
     */
    public void set(int i, boolean value) {
        if (!(0 <= i && i < length)) throw new IndexOutOfBoundsException();
        mask.set(i, value);
    }

    /**
     * Copy the masked bits from {@code other}.
     * {@snippet lang = "Python":
     * # python numpy form
     * this[mask] = other[mask]
     *}
     *
     * @param other other mask
     * @param mask  mask on both.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}, {@code mask}.
     */
    public void set(BlueprintMask other, BlueprintMask mask) {
        if (length != other.length) throw new IllegalArgumentException();
        if (length != mask.length) throw new IllegalArgumentException();
        // (this | mask) & (other & mask)
        this.mask.or(mask.mask);
        this.mask.and(other.and(mask).mask);
    }

    /**
     * Copy the masked bits from {@code other}.
     * <p>
     * {@snippet lang = "Python":
     * # python numpy form
     * this[writeMask] = other[readMask]
     *}
     *
     * @param writeMask mask on self
     * @param other     other mask
     * @param readMask  mask on {@code other}.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}, {@code writeMask} or {@code readMask},
     *                                  or {@code writeMask} and {@code readMask} do not agree on {@link #count()} number.
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

    /**
     * Invert all bits.
     *
     * @return a new mask
     * @see #inot()
     */
    public BlueprintMask not() {
        var ret = new BlueprintMask(this);
        ret.mask.flip(0, length);
        return ret;
    }

    /**
     * Invert all bits inplace.
     *
     * @return this.
     * @see #not()
     */
    public BlueprintMask inot() {
        mask.flip(0, length);
        return this;
    }

    /**
     * And all bits.
     *
     * @param other other mask
     * @return a new mask.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #iand(BlueprintMask)
     */
    public BlueprintMask and(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        return new BlueprintMask(this).iand(other);
    }

    /**
     * And all bits inplace.
     *
     * @param other other mask
     * @return this
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #and(BlueprintMask)
     */
    public BlueprintMask iand(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        mask.and(other.mask);
        return this;
    }

    /**
     * Or all bits.
     *
     * @param other other mask
     * @return a new mask.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #ior(BlueprintMask)
     */
    public BlueprintMask or(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        return new BlueprintMask(this).ior(other);
    }

    /**
     * Or all bits inplace.
     *
     * @param other other mask
     * @return a new mask.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #or(BlueprintMask)
     */
    public BlueprintMask ior(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        mask.or(other.mask);
        return this;
    }

    /**
     * Xor all bits.
     *
     * @param other other mask
     * @return a new mask.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #ixor(BlueprintMask)
     */
    public BlueprintMask xor(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        if (this == other) return new BlueprintMask(length);
        return new BlueprintMask(this).ixor(other);
    }

    /**
     * Xor all bits inplace.
     *
     * @param other other mask
     * @return a new mask.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #xor(BlueprintMask)
     */
    public BlueprintMask ixor(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        mask.xor(other.mask);
        return this;
    }

    /**
     * Diff all bits.
     *
     * @param other other mask
     * @return a new mask.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #idiff(BlueprintMask)
     */
    public BlueprintMask diff(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        if (this == other) return new BlueprintMask(length);
        return and(new BlueprintMask(other).inot());
    }

    /**
     * Diff all bits inplace.
     *
     * @param other other mask
     * @return a new mask.
     * @throws IllegalArgumentException {@link #length} mis-match with {@code other}.
     * @see #diff(BlueprintMask)
     */
    public BlueprintMask idiff(BlueprintMask other) {
        if (length != other.length) throw new IllegalArgumentException();
        if (this == other) {
            mask.clear(0, length);
            return this;
        }
        return iand(new BlueprintMask(other).inot());
    }

    /**
     * Look for the previous clear bit position before {@code fromIndex}.
     *
     * @param fromIndex start index
     * @return index of clear bit. {@code -1} if not found.
     * @see BitSet#previousClearBit(int)
     */
    public int previousClearIndex(int fromIndex) {
        return mask.previousClearBit(fromIndex);
    }

    /**
     * Look for the previous set bit position before {@code fromIndex}.
     *
     * @param fromIndex start index
     * @return index of set bit. {@code -1} if not found.
     * @see BitSet#previousSetBit(int)
     */
    public int previousSetIndex(int fromIndex) {
        return mask.previousSetBit(fromIndex);
    }

    /**
     * Look for the next clear bit position before {@code fromIndex}.
     *
     * @param fromIndex start index
     * @return index of clear bit. {@code -1} if not found.
     * @see BitSet#nextClearBit(int)
     */
    public int nextClearIndex(int fromIndex) {
        return mask.nextClearBit(fromIndex);
    }

    /**
     * Look for the next set bit position before {@code fromIndex}.
     *
     * @param fromIndex start index
     * @return index of set bit. {@code -1} if not found.
     * @see BitSet#nextSetBit(int)
     */
    public int nextSetIndex(int fromIndex) {
        return mask.nextSetBit(fromIndex);
    }

    /**
     * a stream of index of set bits.
     *
     * @return a stream of index of set bits.
     * @see BitSet#stream()
     */
    public IntStream stream() {
        return mask.stream();
    }

    /**
     * For each index of set bits.
     *
     * @param set index consumer.
     */
    public void forEach(IntConsumer set) {
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            set.accept(i);
        }
    }

    /**
     * For each index of clear bits.
     *
     * @param set index consumer.
     */
    public void forEachClear(IntConsumer set) {
        for (int i = nextClearIndex(0); i >= 0; i = nextClearIndex(i + 1)) {
            set.accept(i);
        }
    }

    /**
     * {@return the index array of set bits}
     */
    public int[] asElectrodeIndex() {
        return mask.stream().toArray();
    }

    /**
     * Create a mask from index array.
     *
     * @param total total electrode number.
     * @param index index of electrode.
     * @return a mask
     */
    public static BlueprintMask from(int total, int[] index) {
        var ret = new BlueprintMask(total);
        for (int e : index) {
            ret.set(e);
        }
        return ret;
    }

    /**
     * Create a mask from a subset index array.
     *
     * @param total  total electrode number.
     * @param index  index of electrode.
     * @param offset start
     * @param length length
     * @return a mask
     */
    public static BlueprintMask from(int total, int[] index, int offset, int length) {
        var ret = new BlueprintMask(total);
        for (int i = 0; i < length; i++) {
            ret.set(index[i + offset]);
        }
        return ret;
    }

    /**
     * {@return a boolean array of set bits}
     */
    public boolean[] asBooleanMask() {
        var ret = new boolean[length];
        mask.stream().forEach(i -> ret[i] = true);
        return ret;
    }

    /**
     * Create a mask from boolean array.
     *
     * @param mask boolean array
     * @return a mask
     */
    public static BlueprintMask from(boolean[] mask) {
        var ret = new BlueprintMask(mask.length);
        for (int i = 0, length = mask.length; i < length; i++) {
            if (mask[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask on {@code array}.
     *
     * @param array  data array
     * @param tester tester
     * @return a mask
     */
    public static BlueprintMask from(int[] array, IntPredicate tester) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            ret.set(i, tester.test(array[i]));
        }
        return ret;
    }

    /**
     * Create a mask on {@code array}.
     *
     * @param array  data array
     * @param tester tester
     * @return a mask
     */
    public static BlueprintMask from(double[] array, DoublePredicate tester) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            ret.set(i, tester.test(array[i]));
        }
        return ret;
    }

    /**
     * Create a mask on {@code array}.
     *
     * @param array  data array
     * @param tester tester
     * @param <T>    type of {@code array}
     * @return a mask
     */
    public static <T> BlueprintMask from(T[] array, Predicate<T> tester) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            ret.set(i, tester.test(array[i]));
        }
        return ret;
    }

    /**
     * Create an all set mask.
     *
     * @param length total electrode number
     * @return a mask
     */
    public static BlueprintMask all(int length) {
        var ret = new BlueprintMask(length);
        ret.mask.flip(0, length);
        return ret;
    }

    /**
     * Create an all set mask for {@code array}.
     *
     * @param array data array
     * @return a mask
     */
    public static BlueprintMask all(int[] array) {
        return all(array.length);
    }

    /**
     * Create an all set mask for {@code array}.
     *
     * @param array data array
     * @return a mask
     */
    public static BlueprintMask all(double[] array) {
        return all(array.length);
    }

    /**
     * Create a mask for {@code array} where element in {@code array} equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask eq(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] == value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is not equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask ne(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] != value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask lt(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] < value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than or equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask le(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] <= value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater than {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask gt(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] > value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater than or equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask ge(int[] array, int value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] >= value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask eq(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] == value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is not equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask ne(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] != value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask lt(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] < value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than or equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask le(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] <= value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater than {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask gt(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] > value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater than or equals to {@code value}.
     *
     * @param array data array
     * @param value test value.
     * @return a mask
     */
    public static BlueprintMask ge(double[] array, double value) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (array[i] >= value) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask eq(int[] array, int[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();

        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] == value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is not equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask ne(int[] array, int[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] != value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask lt(int[] array, int[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] < value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than or equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask le(int[] array, int[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] <= value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater then {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask gt(int[] array, int[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] > value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater than or equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask ge(int[] array, int[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] >= value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask eq(double[] array, double[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] == value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is not equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask ne(double[] array, double[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] != value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask lt(double[] array, double[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] < value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is less than or equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask le(double[] array, double[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] <= value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater then {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask gt(double[] array, double[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] > value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask for {@code array} where element in {@code array} is greater than or equals to {@code value} in pair.
     *
     * @param array data array
     * @param value test array.
     * @return a mask
     * @throws IllegalArgumentException {@code array} and {@code value} do not have same length.
     */
    public static BlueprintMask ge(double[] array, double[] value) {
        var length = array.length;
        if (length != value.length) throw new IllegalArgumentException();
        var ret = new BlueprintMask(length);
        for (int i = 0; i < length; i++) {
            if (array[i] >= value[i]) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask that indicate the nan position in {@code array}.
     *
     * @param array data array
     * @return a mask
     * @see #notNan(double[])
     */
    public static BlueprintMask nan(double[] array) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (Double.isNaN(array[i])) ret.set(i);
        }
        return ret;
    }

    /**
     * Create a mask that indicate the non-nan position in {@code array}.
     *
     * @param array data array
     * @return a mask
     * @see #nan(double[])
     */
    public static BlueprintMask notNan(double[] array) {
        var ret = new BlueprintMask(array.length);
        for (int i = 0, length = array.length; i < length; i++) {
            if (!Double.isNaN(array[i])) ret.set(i);
        }
        return ret;
    }

    /**
     * Fill {@code array} with {@code value} under a mask.
     * {@snippet lang = "Python":
     * # python numpy form
     * array[this] = value
     *}
     *
     * @param array data array
     * @param value fill value
     * @throws IllegalArgumentException {@code array} does not have same length.
     */
    public void fill(int[] array, int value) {
        if (length != array.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            array[i] = value;
        }
    }

    /**
     * Fill {@code array} with {@code value} under a mask.
     * {@snippet lang = "Python":
     * # python numpy form
     * array[this] = value
     *}
     *
     * @param array data array
     * @param value fill value
     * @throws IllegalArgumentException {@code array} does not have same length.
     */
    public void fill(double[] array, double value) {
        if (length != array.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            array[i] = value;
        }
    }

    /**
     * Extra value from {@code array} by a mask.
     * {@snippet lang = "Python":
     * # python numpy form
     * array[this]
     *}
     *
     * @param array source array
     * @return output array
     * @throws IllegalArgumentException {@code array} does not have same length.
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
     * Extra value from {@code array} by a mask into {@code output}.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[:this.count()] = array[this]
     *}
     *
     * @param output output array
     * @param array  source array
     * @return number of value filled in {@code output}
     * @throws IllegalArgumentException {@code array} does not have same length,
     *                                  or the length of {@code output} is smaller than {@link #count()}.
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
     * Extra value from {@code array} by a mask.
     * {@snippet lang = "Python":
     * # python numpy form
     * array[this]
     *}
     *
     * @param array source array
     * @return output array
     * @throws IllegalArgumentException {@code array} does not have same length.
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
     * Extra value from {@code array} by a mask into {@code output}.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[:this.count()] = array[this]
     *}
     *
     * @param output output array
     * @param array  source array
     * @return number of value filled in {@code output}
     * @throws IllegalArgumentException {@code array} does not have same length,
     *                                  or the length of {@code output} is smaller than {@link #count()}.
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
     * Take value from {@code array} with a mask.
     * {@snippet lang = "Python":
     * # python numpy form
     * np.where(this, array, otherwise)
     *}
     *
     * @param array     source array
     * @param otherwise fallback value
     * @return a new array
     * @throws IllegalArgumentException {@code array} does not have same length,
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
     * Take value from {@code array} with a mask into {@code output}.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[this] = array[this]
     *}
     *
     * @param output output array
     * @param array  source array
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length.
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
     * Take value from {@code array} with a mask into {@code output}.
     * {@code output} will be filled by {@code otherwise} first.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[:] = np.where(this, array, otherwise)
     *}
     *
     * @param output    output array
     * @param array     source array
     * @param otherwise false value
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length.
     * @throws RuntimeException         {@code output} and {@code array} are the same object.
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
     * Take value from {@code array} with a mask into {@code output} with another {@code mask}.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[this] = array[mask]
     *}
     *
     * @param output output array
     * @param array  source array
     * @param mask   mask on {@code array}. If it is {@code null}, it works like {@link #all(int[]) all}({@code array}).
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length,
     *                                  or {@code mask} do not agree on {@link #count()} number with this mask.
     * @throws RuntimeException         {@code output} and {@code array} are the same object.
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
     * Take value from {@code array} with a mask.
     * {@snippet lang = "Python":
     * # python numpy form
     * np.where(this, array, otherwise)
     *}
     * {@code np.where(this, array, otherwise)}
     *
     * @param array     source array
     * @param otherwise fallback value
     * @return a new array
     * @throws IllegalArgumentException {@code array} does not have same length,
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
     * Take value from {@code array} with a mask into {@code output}.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[this] = array[this]
     *}
     * {@code np.where(this, array, otherwise)}
     *
     * @param output output array
     * @param array  source array
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length.
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
     * Take value from {@code array} with a mask into {@code output}.
     * {@code output} will be filled by {@code otherwise} first.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[:] = np.where(this, array, otherwise)
     *}
     *
     * @param output    output array
     * @param array     source array
     * @param otherwise false value
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length.
     * @throws RuntimeException         {@code output} and {@code array} are the same object.
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
     * Take value from {@code array} with a mask into {@code output} with another {@code mask}.
     * {@snippet lang = "Python":
     * # python numpy form
     * output[this] = array[mask]
     *}
     *
     * @param output output array
     * @param array  source array
     * @param mask   mask on {@code array}. If it is {@code null}, it works like {@link #all(int[]) all}({@code array}).
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length,
     *                                  or {@code mask} do not agree on {@link #count()} number with this mask.
     * @throws RuntimeException         {@code output} and {@code array} are the same object.
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

    /**
     * Partial apply {@code oper} onto {@code array}. Otherwise, fill with zero.
     *
     * @param array data array.
     * @param oper  value operator
     * @return a new array.
     * @throws IllegalArgumentException {@code array} does not have same length.
     */
    public int[] map(int[] array, IntUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = new int[length];
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret[i] = oper.applyAsInt(array[i]);
        }
        return ret;
    }

    /**
     * Partial apply {@code oper} onto {@code array} and put result into {@code output}.
     * Otherwise, keep unchange.
     *
     * @param output output array.
     * @param array  data array.
     * @param oper   value operator
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length.
     */
    public void map(int[] output, int[] array, IntUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        if (length != output.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            output[i] = oper.applyAsInt(array[i]);
        }
    }

    /**
     * Partial apply {@code oper} onto {@code array}. Otherwise, fill with zero.
     *
     * @param array data array.
     * @param oper  value operator
     * @return a new array.
     * @throws IllegalArgumentException {@code array} does not have same length.
     */
    public double[] map(double[] array, DoubleUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        var ret = new double[length];
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            ret[i] = oper.applyAsDouble(array[i]);
        }
        return ret;
    }

    /**
     * Partial apply {@code oper} onto {@code array} and put result into {@code output}.
     * Otherwise, keep unchange.
     *
     * @param output output array.
     * @param array  data array.
     * @param oper   value operator
     * @throws IllegalArgumentException {@code output} or {@code array} does not have same length.
     */
    public void map(double[] output, double[] array, DoubleUnaryOperator oper) {
        if (length != array.length) throw new IllegalArgumentException();
        if (length != output.length) throw new IllegalArgumentException();
        for (int i = nextSetIndex(0); i >= 0; i = nextSetIndex(i + 1)) {
            output[i] = oper.applyAsDouble(array[i]);
        }
    }

    /**
     * Fold partial value from {@code array}.
     *
     * @param array data array
     * @param oper  value operator
     * @return result.
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
     * Fold partial value from {@code array}.
     *
     * @param array data array
     * @param init  initial value
     * @param oper  operator
     * @return result
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
     * Fold partial value from {@code array}.
     *
     * @param array data array
     * @param oper  operator
     * @return result
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
     * Fold partial value from {@code array}.
     *
     * @param array data array
     * @param init  initial value
     * @param oper  operator
     * @return result
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
