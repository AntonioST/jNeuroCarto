package io.ast.jneurocarto.javafx.script;


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
    PyTuple0 EMPTY_TUPLE = new PyTuple0();

    /**
     * represent Python {@code {}}.
     */
    PyDict EMPTY_DICT = new PyDict(List.of(), List.of());

    static PyValue valueOf(@Nullable Object object) {
        return switch (object) {
            case null -> None;
            case boolean b -> b ? True : False;
            case int i -> new PyInt(i);
//            case long i -> new PyInt(i);
            case double f -> new PyFloat(f);
            case String s -> new PyStr(s);
            case List<?> list -> new PyList(list.stream().map(PyValue::valueOf).toList());
            case Set<?> set -> new PySet(set.stream().map(PyValue::valueOf).collect(Collectors.toSet()));
            case Map<?, ?> map -> {
                if (map.isEmpty()) {
                    yield new PyDict();
                } else {
                    var k = new ArrayList<String>();
                    var v = new ArrayList<PyValue>();
                    for (var e : map.entrySet()) {
                        if (e.getKey() instanceof String ek) {
                            k.add(ek);
                            v.add(valueOf(e.getValue()));
                        } else {
                            throw new RuntimeException("map key not a string : " + e.getKey());
                        }
                    }
                    yield new PyDict(k, v);
                }
            }
            case PyValue v -> v;
            default -> throw new RuntimeException("not a python representable literal value : " + object);
        };
    }

    /**
     * represent Python {@code None} type.
     */
    record PyNone() implements PyValue {
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
    record PyBool(boolean value) implements PyValue {
        @Override
        public String toString() {
            return "bool(" + value + ")";
        }
    }

    /**
     * represent Python {@code int} type.
     *
     * @param value int value.
     */
    record PyInt(int value) implements PyValue {
        public PyBool asBool() {
            return value == 0 ? PyValue.False : PyValue.True;
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
    record PyFloat(double value) implements PyValue {
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
         * {@return the type description of the collection.}
         */
        String rawTypeStr();

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
        default List<PyTuple2<PyInt, PyValue>> enumerate() {
            var iter = iter();
            return IntStream.range(0, iter.size())
                .mapToObj(i -> new PyTuple2<>(new PyInt(i), iter.get(i)))
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
                if (elements.get(i) instanceof PyInt(int value)) {
                    ret[i] = value;
                } else {
                    throw new RuntimeException("not a python int " + rawTypeStr());
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
                case PyInt(var value) -> ret[i] = value;
                case PyFloat(var value) -> ret[i] = value;
                default -> throw new RuntimeException("not a python float " + rawTypeStr());
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
                    throw new RuntimeException("not a python str " + rawTypeStr());
                }
            }
            return ret;
        }

        /**
         * A helper method for {@code toString()}.
         *
         * @return a comma-separated list of stringified elements.
         */
        default String toStringIter() {
            return iter().stream().map(PyValue::toString).collect(Collectors.joining(", "));
        }
    }

    /**
     * represent Python {@code tuple} type.
     */
    sealed interface PyTuple extends PyIterable {
        @Override
        default String rawTypeStr() {
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

        @Override
        default String toStringIter() {
            return "(" + PyIterable.super.toStringIter() + ")";
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
        public String toString() {
            return PyTuple.super.toStringIter();
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
        public String toString() {
            return PyTuple.super.toStringIter();
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
            return PyTuple.super.toStringIter();
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
        /**
         * Create an empty list.
         */
        public PyList() {
            this(List.of());
        }

        /**
         * Create a list from array.
         *
         * @param elements elements
         */
        public PyList(PyValue... elements) {
            this(Arrays.asList(elements));
        }

        @Override
        public String rawTypeStr() {
            return "list";
        }

        @Override
        public List<PyValue> iter() {
            return elements;
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
            return "[" + PyIterable.super.toStringIter() + "]";
        }
    }

    /**
     * represent Python {@code set[Any]} type.
     *
     * @param elements value list.
     */
    record PySet(Set<PyValue> elements) implements PyIterable {

        /**
         * Create an empty dict.
         */
        public PySet() {
            this(Set.of());
        }

        @Override
        public String rawTypeStr() {
            return "set";
        }

        @Override
        public int size() {
            return elements.size();
        }

        @Override
        public List<PyValue> iter() {
            return new ArrayList<>(elements);
        }

        @Override
        public String toString() {
            return elements.stream().map(PyValue::toString).collect(Collectors.joining(", ", "{", "}"));
        }
    }

    /**
     * represent Python {@code dict[str, Any]} type.
     *
     * @param keys     key name list
     * @param elements value list.
     */
    record PyDict(List<String> keys, List<PyValue> elements) implements PyIterable {

        public PyDict {
            if (keys.size() != new HashSet<>(keys).size()) throw new RuntimeException("duplicate keys");
            if (keys.size() != elements.size()) throw new RuntimeException();
        }

        /**
         * Create an empty dict.
         */
        public PyDict() {
            this(List.of(), List.of());
        }

        /**
         * Create a dict from a map;
         *
         * @param maps map
         */
        public PyDict(Map<String, PyValue> maps) {
            var keys = new ArrayList<String>(maps.size());
            var elements = new ArrayList<PyValue>(maps.size());
            for (var e : maps.entrySet()) {
                keys.add(e.getKey());
                elements.add(e.getValue());
            }
            this(keys, elements);
        }

        @Override
        public String rawTypeStr() {
            return "dict";
        }

        @Override
        public int size() {
            return elements.size();
        }

        /**
         * {@return a java {@link List} of keys.}
         */
        @Override
        public List<PyValue> iter() {
            return keys.stream().map(it -> (PyValue) new PyStr(it)).toList();
        }

        /**
         * likes Python {@code dict.items}
         *
         * @return a list of {@code (key, value)}
         */
        public List<PyTuple2<PyStr, PyValue>> items() {
            return IntStream.range(0, keys.size()).mapToObj(i -> {
                var k = keys.get(i);
                var v = elements.get(i);
                return new PyTuple2<>(new PyStr(k), v);
            }).toList();
        }

        @Override
        public PyValue get(int i) {
            return new PyStr(keys.get(i));
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
            return elements.get(i);
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof String k) {
                return keys.contains(k);
            } else if (object instanceof PyStr(var k)) {
                return keys.contains(k);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return IntStream.range(0, keys.size()).mapToObj(i -> {
                var k = keys.get(i);
                var v = elements.get(i);
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
    record PyStr(String string) implements PyValue {
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
