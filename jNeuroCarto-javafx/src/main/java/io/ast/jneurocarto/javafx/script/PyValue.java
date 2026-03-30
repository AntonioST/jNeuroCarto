package io.ast.jneurocarto.javafx.script;


import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * represent a subset of Python literal value type.
 */
@NullMarked
public sealed interface PyValue {

    /**
     * represent Python {@code None}.
     */
    PyNone None = new PyNone();

    /**
     * represent Python {@code True}.
     */
    PyBool True = new PyBool(true);

    /**
     * represent Python {@code False}.
     */
    PyBool False = new PyBool(false);

    /**
     * represent Python {@code []}.
     */
    PyList EMPTY_LIST = new PyList(List.of());

    /**
     * represent Python {@code ()}.
     */
    PyTuple EMPTY_TUPLE = new PyTuple0();

    /**
     * represent Python {@code {}}.
     */
    PySet EMPTY_SET = new PySet(List.of());

    /**
     * represent Python {@code {}}.
     */
    PyDict EMPTY_DICT = new PyGeneralDict(List.of(), List.of());

    String type();

    static PyValue valueOf(@Nullable Object object) {
        return switch (object) {
            case null -> None;
            case boolean b -> b ? True : False;
            case int i -> new PyInt32(i);
            case long i -> new PyInt64(i);
            case BigInteger i -> new PyIntBig(i);
            case double f -> new PyFloat(f);
            case String s -> new PyStr(s);
            case List<?> list when list.isEmpty() -> EMPTY_LIST;
            case List<?> list -> new PyList(list.stream().map(PyValue::valueOf).toList());
            case Set<?> set when set.isEmpty() -> EMPTY_SET;
            case Set<?> set -> new PySet(set.stream().map(PyValue::valueOf).collect(Collectors.toList()));
            case Map<?, ?> map when map.isEmpty() -> EMPTY_DICT;
            case Map<?, ?> map when isAllStrKeys(map) -> {
                var k = new ArrayList<String>();
                var v = new ArrayList<PyValue>();
                for (var e : map.entrySet()) {
                    k.add((String) e.getKey());
                    v.add(valueOf(e.getValue()));
                }
                yield new PyStrDict(k, v);
            }
            case Map<?, ?> map -> {
                var k = new ArrayList<PyValue>();
                var v = new ArrayList<PyValue>();
                for (var e : map.entrySet()) {
                    k.add(valueOf(e.getKey()));
                    v.add(valueOf(e.getValue()));
                }
                yield new PyGeneralDict(k, v);
            }
            case PyValue v -> v;
            default -> throw new RuntimeException("not a python representable literal value : " + object);
        };
    }

    private static boolean isAllStrKeys(Map<?, ?> map) {
        for (var o : map.keySet()) {
            if (!(o instanceof String)) return false;
        }
        return true;
    }

    interface PyHashable {
    }

    /**
     * represent Python {@code None} type.
     */
    record PyNone() implements PyValue, PyHashable {
        @Override
        public String type() {
            return "NoneType";
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    /**
     * represent Python {@code bool} type.
     *
     * @param value boolean value.
     */
    record PyBool(boolean value) implements PyValue, PyHashable {
        @Override
        public String type() {
            return "bool";
        }

        @Override
        public String toString() {
            return "bool(" + value + ")";
        }
    }

    /**
     * represent Python {@code int} type.
     */
    sealed interface PyInt extends PyValue, PyHashable {

        static PyInt of(int value) {
            return new PyInt32(value);
        }

        static PyInt of(long value) {
            return new PyInt64(value);
        }

        static PyInt of(BigInteger value) {
            return new PyIntBig(value);
        }

        @Override
        default String type() {
            return "int";
        }

        /**
         * {@return is it a zero value}
         */
        PyBool asBool();

        /**
         * try cast to java int.
         *
         * @return int  value
         * @throws ArithmeticException if value over int domain
         */
        int toInt();

        /**
         * try cast to java long.
         *
         * @return long  value
         * @throws ArithmeticException if value over long domain.
         */
        long toLong();

        /**
         * cast to java double.
         *
         * @return double value
         */
        double toDouble();

        /**
         * cast to {@link BigInteger}.
         *
         * @return value
         */
        BigInteger toBigInteger();

        static boolean equals(PyInt i1, PyInt i2) {
            try {
                return i1.toInt() == i2.toInt();
            } catch (ArithmeticException e) {
            }
            try {
                return i1.toLong() == i2.toLong();
            } catch (ArithmeticException e) {
            }
            return i1.toBigInteger().equals(i2.toBigInteger());
        }
    }

    /**
     * represent Python {@code int} type.
     *
     * @param value int value.
     */
    record PyInt32(int value) implements PyInt {
        @Override
        public PyBool asBool() {
            return value == 0 ? PyValue.False : PyValue.True;
        }

        @Override
        public int toInt() {
            return value;
        }

        @Override
        public long toLong() {
            return value;
        }

        @Override
        public double toDouble() {
            return value;
        }

        @Override
        public BigInteger toBigInteger() {
            return BigInteger.valueOf(value);
        }

        @Override
        public String toString() {
            return "int(" + value + ")";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PyInt that && PyInt.equals(this, that);
        }
    }

    /**
     * represent Python {@code int} type.
     *
     * @param value long value.
     */
    record PyInt64(long value) implements PyInt {
        @Override
        public PyBool asBool() {
            return value == 0 ? PyValue.False : PyValue.True;
        }

        @Override
        public int toInt() {
            if (value instanceof int ret) return ret;
            throw new ArithmeticException("cannot cast to int");
        }

        @Override
        public long toLong() {
            return value;
        }

        @Override
        public double toDouble() {
            return value;
        }

        @Override
        public BigInteger toBigInteger() {
            return BigInteger.valueOf(value);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PyInt that && PyInt.equals(this, that);
        }

        @Override
        public String toString() {
            return "int(" + value + ")";
        }
    }

    /**
     * represent Python {@code int} type.
     *
     * @param value BigInteger value.
     */
    record PyIntBig(BigInteger value) implements PyInt {
        @Override
        public PyBool asBool() {
            return value.compareTo(BigInteger.ZERO) == 0 ? PyValue.False : PyValue.True;
        }

        @Override
        public int toInt() {
            return value.intValueExact();
        }

        @Override
        public long toLong() {
            return value.longValueExact();
        }

        @Override
        public double toDouble() {
            return value.doubleValue();
        }

        @Override
        public BigInteger toBigInteger() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PyInt that && PyInt.equals(this, that);
        }

        @Override
        public String toString() {
            return "int(" + value + ")";
        }
    }

    /**
     * represent Python {@code float} type.
     *
     * @param value double value
     */
    record PyFloat(double value) implements PyValue, PyHashable {

        public static PyFloat of(float value) {
            return new PyFloat(value);
        }

        public static PyFloat of(double value) {
            return new PyFloat(value);
        }

        @Override
        public String type() {
            return "float";
        }

        @Override
        public String toString() {
            return "float(" + value + ")";
        }
    }

    /**
     * represent Python {@code Iterable} type.
     * <br/>
     * However, it is designed more like a {@code Collection<T>}.
     */
    sealed interface PyIterable extends PyValue {

        /**
         * {@return a java {@link List} of value.}
         */
        List<PyValue> iter();

        /**
         * {@return size of the collection}
         */
        default int size() {
            return iter().size();
        }

        /**
         * get element at given index.
         *
         * @param i index.
         * @return value at {@code i}
         */
        default PyValue get(int i) {
            return iter().get(i);
        }

        default boolean contains(Object object) {
            PyValue value;
            try {
                value = valueOf(object);
            } catch (RuntimeException e) {
                return false;
            }
            return iter().contains(value);
        }

        /**
         * {@return is it not empty?}
         */
        default PyBool asBool() {
            return size() > 0 ? PyValue.True : PyValue.False;
        }

        /**
         * likes Python {@code enumerate(this)}.
         *
         * @return a list of {@code (int, T)}
         */
        default List<PyTuple2<PyInt32, PyValue>> enumerate() {
            var iter = iter();
            return IntStream.range(0, iter.size())
              .mapToObj(i -> new PyTuple2<>(new PyInt32(i), iter.get(i)))
              .toList();
        }

        /**
         * try to cast to {@code int[]}.
         *
         * @return int array
         * @throws RuntimeException any element is not a {@link PyInt}.
         */
        default int[] toIntArray() {
            var elements = iter();
            var size = elements.size();
            var ret = new int[size];
            for (int i = 0; i < size; i++) {
                if (elements.get(i) instanceof PyInt pi) {
                    ret[i] = pi.toInt();
                } else {
                    throw new RuntimeException("not a python int " + type());
                }
            }
            return ret;
        }

        /**
         * try to cast to {@code double[]}.
         *
         * @return double array
         * @throws RuntimeException any element is not a {@link PyInt} nor {@link PyFloat}.
         */
        default double[] toDoubleArray() {
            var elements = iter();
            var size = elements.size();
            var ret = new double[size];
            for (int i = 0; i < size; i++) {
                switch (elements.get(i)) {
                case PyInt pi -> ret[i] = pi.toDouble();
                case PyFloat(var value) -> ret[i] = value;
                default -> throw new RuntimeException("not a python float " + type());
                }
            }
            return ret;
        }

        /**
         * try to cast to {@code String[]}.
         *
         * @return {@link String} array
         * @throws RuntimeException any element is not a {@link PyStr}.
         */
        default String[] toStringArray() {
            var elements = iter();
            var size = elements.size();
            var ret = new String[size];
            for (int i = 0; i < size; i++) {
                if (elements.get(i) instanceof PyStr(var value)) {
                    ret[i] = value;
                } else {
                    throw new RuntimeException("not a python str " + type());
                }
            }
            return ret;
        }

        /**
         * A helper method for {@code toString()}.
         *
         * @param prefix
         * @param suffix
         * @return a comma-separated list of stringified elements.
         */
        default String toStringIter(String prefix, String suffix) {
            return iter().stream().map(PyValue::toString).collect(Collectors.joining(", ", prefix, suffix));
        }
    }

    /**
     * represent Python {@code tuple} type.
     */
    sealed interface PyTuple extends PyIterable, PyHashable {

        static PyTuple of() {
            return EMPTY_TUPLE;
        }

        static PyTuple of(PyValue v) {
            return new PyTuple1<>(v);
        }

        static PyTuple of(PyValue v1, PyValue v2) {
            return new PyTuple2<>(v1, v2);
        }

        static PyTuple of(PyValue v1, PyValue v2, PyValue v3) {
            return new PyTuple3<>(v1, v2, v3);
        }

        static PyTuple of(List<PyValue> elements) {
            return switch (elements.size()) {
                case 0 -> EMPTY_TUPLE;
                case 1 -> new PyTuple1<>(elements.get(0));
                case 2 -> new PyTuple2<>(elements.get(0), elements.get(1));
                case 3 -> new PyTuple3<>(elements.get(0), elements.get(1), elements.get(2));
                default -> new PyTupleN(elements);
            };
        }


        @Override
        default String type() {
            return "tuple";
        }

        /**
         * cast to {@link PyList}.
         *
         * @return a list
         */
        default PyList asList() {
            return new PyList(iter());
        }
    }

    /**
     * represent Python {@code ()} (empty tuple) type.
     */
    record PyTuple0() implements PyTuple {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public List<PyValue> iter() {
            return List.of();
        }

        @Override
        public boolean contains(Object object) {
            return false;
        }

        @Override
        public PyList asList() {
            return PyValue.EMPTY_LIST;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PyTuple t && t.size() == 0;
        }

        @Override
        public String toString() {
            return "()";
        }
    }

    /**
     * represent Python {@code tuple[Any]} type.
     *
     * @param value value
     */
    record PyTuple1<T extends PyValue>(T value) implements PyTuple {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public List<PyValue> iter() {
            return List.of(value);
        }

        @Override
        public boolean contains(Object object) {
            try {
                return value.equals(valueOf(object));
            } catch (RuntimeException e) {
                return false;
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PyTuple t && t.size() == 1 && value.equals(t.get(0));
        }

        @Override
        public String toString() {
            return "(" + value + ",)";
        }
    }

    /**
     * represent Python {@code tuple[Any, Any]} type.
     *
     * @param first  value
     * @param second value
     */
    record PyTuple2<T1 extends PyValue, T2 extends PyValue>(T1 first, T2 second) implements PyTuple {
        @Override
        public int size() {
            return 2;
        }

        @Override
        public List<PyValue> iter() {
            return List.of(first, second);
        }

        @Override
        public boolean contains(Object object) {
            PyValue value;
            try {
                value = valueOf(object);
            } catch (RuntimeException e) {
                return false;
            }
            return first.equals(value) || second.equals(value);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PyTuple t
                   && t.size() == 2
                   && first.equals(t.get(0))
                   && second.equals(t.get(1));
        }

        @Override
        public String toString() {
            return toStringIter("(", ")");
        }
    }

    /**
     * represent Python {@code tuple[Any, Any, Any]} type.
     *
     * @param first  value
     * @param second value
     * @param third  value
     */
    record PyTuple3<T1 extends PyValue, T2 extends PyValue, T3 extends PyValue>(T1 first, T2 second, T3 third) implements PyTuple {
        @Override
        public int size() {
            return 3;
        }

        @Override
        public List<PyValue> iter() {
            return List.of(first, second, third);
        }

        @Override
        public boolean contains(Object object) {
            PyValue value;
            try {
                value = valueOf(object);
            } catch (RuntimeException e) {
                return false;
            }
            return first.equals(value) || second.equals(value) || third.equals(value);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PyTuple t
                   && t.size() == 3
                   && first.equals(t.get(0))
                   && second.equals(t.get(1))
                   && third.equals(t.get(2));
        }

        @Override
        public String toString() {
            return toStringIter("(", ")");
        }
    }

    /**
     * represent Python {@code tuple[Any, ...]} type.
     *
     * @param elements value list
     */
    record PyTupleN(List<PyValue> elements) implements PyTuple {
        @Override
        public List<PyValue> iter() {
            return elements;
        }

        @Override
        public String toString() {
            return toStringIter("(", ")");
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PyTuple other)) return false;
            if (size() != other.size()) return false;
            if (size() == 0) return true;
            return elements.equals(other.iter());
        }
    }

    /**
     * represent Python {@code list[Any]} type.
     *
     * @param elements value list
     */
    record PyList(List<PyValue> elements) implements PyIterable {
        public static PyList of() {
            return EMPTY_LIST;
        }

        public static PyList of(PyValue v) {
            return new PyList(List.of(v));
        }

        public static PyList of(PyValue... values) {
            return new PyList(Arrays.asList(values));
        }

        @Override
        public String type() {
            return "list";
        }

        @Override
        public List<PyValue> iter() {
            return Collections.unmodifiableList(elements);
        }

        /**
         * cast to {@link PyTuple}.
         *
         * @return a tuple.
         */
        public PyTuple asTuple() {
            return switch (elements.size()) {
                case 0 -> PyValue.EMPTY_TUPLE;
                case 1 -> new PyTuple1<>(elements.get(0));
                case 2 -> new PyTuple2<>(elements.get(0), elements.get(1));
                case 3 -> new PyTuple3<>(elements.get(0), elements.get(1), elements.get(2));
                default -> new PyTupleN(elements);
            };
        }

        @Override
        public String toString() {
            return toStringIter("[", "]");
        }
    }

    /**
     * represent Python {@code set[Any]} type.
     *
     * @param elements value list.
     */
    record PySet(List<PyValue> elements) implements PyIterable {

        public PySet {
            var set = new HashSet<PyValue>();
            for (var key : elements) {
                if (!(key instanceof PyHashable)) {
                    throw new RuntimeException("unhashable type : " + key.type());
                } else if (!set.add(key)) {
                    throw new RuntimeException("duplicate key : " + key);
                }
            }
        }

        public static PySet of() {
            return EMPTY_SET;
        }

        public static PySet of(PyValue v) {
            return new PySet(List.of(v));
        }

        public static PySet of(PyValue... values) {
            return new PySet(Arrays.asList(values));
        }

        @Override
        public String type() {
            return "set";
        }

        @Override
        public int size() {
            return elements.size();
        }

        @Override
        public List<PyValue> iter() {
            return Collections.unmodifiableList(elements);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PySet set)) return false;
            if (size() != set.size()) return false;
            return new HashSet<>(elements).containsAll(set.elements);
        }

        @Override
        public String toString() {
            return toStringIter("{", "}");
        }
    }

    /**
     * represent Python {@code dict[Any, Any]} type.
     */
    sealed interface PyDict extends PyIterable {

        static PyDict of() {
            return EMPTY_DICT;
        }

        static PyDict of(PyValue k, PyValue v) {
            return new PyGeneralDict(List.of(k), List.of(v));
        }

        static PyDict of(String k, PyValue v) {
            return new PyStrDict(List.of(k), List.of(v));
        }

        static PyDict of(PyValue k1, PyValue v1,
                         PyValue k2, PyValue v2) {
            return new PyGeneralDict(List.of(k1, k2), List.of(v1, v2));
        }

        static PyDict of(String k1, PyValue v1,
                         String k2, PyValue v2) {
            return new PyStrDict(List.of(k1, k2), List.of(v1, v2));
        }

        @Override
        default String type() {
            return "dict";
        }

        /**
         * {@return a java {@link List} of keys.}
         */
        List<PyValue> keys();

        /**
         * {@return a java {@link List} of values.}
         */
        List<PyValue> values();

        @Override
        default List<PyValue> iter() {
            return Collections.unmodifiableList(keys());
        }

        /**
         * likes Python {@code dict.items}
         *
         * @return a list of {@code (key, value)}
         */
        default List<PyTuple2<PyValue, PyValue>> items() {
            var k = keys();
            var v = values();
            return IntStream.range(0, k.size()).mapToObj(i -> {
                var kk = k.get(i);
                var vv = v.get(i);
                return new PyTuple2<>(kk, vv);
            }).toList();
        }

        @Override
        default PyValue get(int i) {
            return keys().get(i);
        }

        @Override
        default boolean contains(Object object) {
            return keys().contains(object);
        }

        /**
         * likes Python {@code dict.__getitem__}
         *
         * @param key an associated key
         * @return corresponded value
         * @throws NoSuchElementException {@code key} not in the dict.
         */
        default PyValue get(PyValue key) {
            var i = keys().indexOf(key);
            if (i < 0) throw new NoSuchElementException();
            return values().get(i);
        }
    }

    /**
     * represent Python {@code dict[Any, Any]} type.
     */
    record PyGeneralDict(List<PyValue> keys, List<PyValue> values) implements PyDict {
        public PyGeneralDict {
            var set = new HashSet<PyValue>();
            for (var key : keys) {
                if (!(key instanceof PyHashable)) {
                    throw new RuntimeException("unhashable type : " + key.type());
                } else if (!set.add(key)) {
                    throw new RuntimeException("duplicate key : " + key);
                }
            }
            if (keys.size() != values.size()) throw new RuntimeException();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PyDict set)) return false;
            if (size() != set.size()) return false;
            return new HashSet<>(keys()).containsAll(keys());
        }

        @Override
        public String toString() {
            return IntStream.range(0, keys.size()).mapToObj(i -> {
                var k = keys.get(i);
                var v = values.get(i);
                return k + ": " + v;
            }).collect(Collectors.joining(", ", "{", "}"));
        }
    }

    /**
     * represent Python {@code dict[str, Any]} type.
     *
     * @param strkeys key name list
     * @param values  value list.
     */
    record PyStrDict(List<String> strkeys, List<PyValue> values) implements PyDict {

        public PyStrDict {
            if (strkeys.size() != new HashSet<>(strkeys).size()) throw new RuntimeException("duplicate keys");
            if (strkeys.size() != values.size()) throw new RuntimeException();
        }

        /**
         * Create an empty dict.
         */
        public PyStrDict() {
            this(List.of(), List.of());
        }

        /**
         * Create a dict from a String map;
         *
         * @param maps map
         */
        public PyStrDict(Map<String, PyValue> maps) {
            var keys = new ArrayList<String>(maps.size());
            var elements = new ArrayList<PyValue>(maps.size());
            for (var e : maps.entrySet()) {
                keys.add(e.getKey());
                elements.add(e.getValue());
            }
            this(keys, elements);
        }

        @Override
        public int size() {
            return strkeys.size();
        }

        @Override
        public List<PyValue> keys() {
            return strkeys.stream().map(s -> (PyValue) new PyStr(s)).toList();
        }

        /**
         * likes Python {@code dict.items}
         *
         * @return a list of {@code (key, value)}
         */
        public List<PyTuple2<PyValue, PyValue>> items() {
            return IntStream.range(0, strkeys.size()).mapToObj(i -> {
                var k = strkeys.get(i);
                var v = values.get(i);
                return new PyTuple2<PyValue, PyValue>(new PyStr(k), v);
            }).toList();
        }

        @Override
        public PyValue get(int i) {
            return new PyStr(strkeys.get(i));
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof String k) {
                return strkeys.contains(k);
            } else if (object instanceof PyStr(var k)) {
                return strkeys.contains(k);
            } else {
                return false;
            }
        }

        /**
         * Get associated value.
         *
         * @param key key
         * @return value
         */
        public @Nullable PyValue get(String key) {
            var i = key.indexOf(key);
            if (i < 0) return null;
            return values.get(i);
        }

        public PyValue get(PyValue key) {
            if (key instanceof PyStr(var k)) {
                var i = strkeys.indexOf(k);
                if (i >= 0) {
                    return values.get(i);
                }
            }
            throw new NoSuchElementException();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PyDict set)) return false;
            if (size() != set.size()) return false;
            if (obj instanceof PyStrDict dict) {
                return new HashSet<>(strkeys).containsAll(dict.strkeys);
            } else {
                return new HashSet<>(keys()).containsAll(keys());
            }
        }

        @Override
        public String toString() {
            return IntStream.range(0, strkeys.size()).mapToObj(i -> {
                var k = strkeys.get(i);
                var v = values.get(i);
                return k + ": " + v;
            }).collect(Collectors.joining(", ", "{", "}"));
        }
    }


    /**
     * represent unsolved input.
     *
     * @param token
     */
    record PyToken(String token) implements PyValue {
        @Override
        public String type() {
            return "symbol";
        }

        /**
         * {@return {@link #token} length}
         */
        public int length() {
            return token.length();
        }

        /**
         * cast to {@link PyStr}.
         *
         * @return a str
         */
        public PyStr asStr() {
            return new PyStr(token);
        }

        @Override
        public String toString() {
            return "token(" + token + ")";
        }
    }

    /**
     * represent Python {@code str} type.
     *
     * @param string content
     */
    record PyStr(String string) implements PyValue, PyHashable {

        public static PyStr of(CharSequence str) {
            return new PyStr(str.toString());
        }

        @Override
        public String type() {
            return "str";
        }

        /**
         * {@return {@link #string} length}
         */
        public int length() {
            return string.length();
        }

        /**
         * {@return isn't an empty string}
         */
        public PyBool asBool() {
            return length() >= 0 ? PyValue.True : PyValue.False;
        }

        @Override
        public String toString() {
            return "'" + string + "'";
        }
    }

    /**
     * python argument.
     */
    sealed interface PyParameter extends PyValue {

        @Override
        default String type() {
            return "parameter";
        }

        /**
         * {@return argument raw string.}
         */
        String text();

        /**
         * index of {@link #text()} from the origin text.
         *
         * @return index of {@link #text()} from the origin text. {@code -1} if the origin text is unavailable.
         */
        int offset();

        /**
         * {@return parsed non-{@link PyParameter} {@link PyValue}.}
         */
        @Nullable
        PyValue value();
    }

    /**
     * python positional argument.
     *
     * @param index  position index of this parameter.
     * @param text   argument raw string.
     * @param offset index of {@code text} from the origin text. {@code -1} if the origin text is unavailable.
     * @param value  parsed non-{@link PyParameter} {@link PyValue}.
     */
    record PyIndexParameter(int index, String text, int offset, @Nullable PyValue value) implements PyParameter {
        @Override
        public String toString() {
            return index + "=" + value;
        }
    }

    /**
     * python keyword argument.
     *
     * @param name   name of this argument.
     * @param text   argument raw string.
     * @param offset index of {@code text} from the origin text. {@code -1} if the origin text is unavailable.
     * @param value  parsed non-{@link PyParameter} {@link PyValue}.
     */
    record PyNamedParameter(String name, String text, int offset, @Nullable PyValue value) implements PyParameter {
        @Override
        public String toString() {
            return name + "=" + value;
        }
    }


}
