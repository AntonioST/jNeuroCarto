package io.ast.jneurocarto.javafx.script;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface PyValue {

    PyNone None = new PyNone();
    PyBool True = new PyBool(true);
    PyBool False = new PyBool(false);
    PyList EMPTY_LIST = new PyList(List.of());
    PyTuple0 EMPTY_TUPLE = new PyTuple0();
    PyDict EMPTY_DICT = new PyDict(List.of(), List.of());

    /**
     * represent Python {@code None}
     */
    record PyNone() implements PyValue {
        @Override
        public String toString() {
            return "None";
        }
    }

    /**
     * represent Python {@code bool}
     */
    record PyBool(boolean value) implements PyValue {
        @Override
        public String toString() {
            return "bool(" + value + ")";
        }
    }

    /**
     * represent Python {@code int}
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
     * represent Python {@code float}
     */
    record PyFloat(double value) implements PyValue {
        @Override
        public String toString() {
            return "float(" + value + ")";
        }
    }

    /**
     * represent Python {@code Iterable}
     */
    sealed interface PyIterable extends PyValue {
        String rawTypeStr();

        List<PyValue> iter();

        default int size() {
            return iter().size();
        }

        default PyValue get(int i) {
            return iter().get(i);
        }

        default PyBool asBool() {
            return size() == 0 ? PyValue.False : PyValue.True;
        }

        default List<PyTuple2<PyInt, PyValue>> enumerate() {
            var iter = iter();
            return IntStream.range(0, iter.size())
                .mapToObj(i -> new PyTuple2<>(new PyInt(i), iter.get(i)))
                .toList();
        }

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

        default String toStringIter() {
            return iter().stream().map(PyValue::toString).collect(Collectors.joining(", "));
        }
    }

    /**
     * represent Python {@code tuple}
     */
    sealed interface PyTuple extends PyIterable {
        @Override
        default String rawTypeStr() {
            return "tuple";
        }

        default PyList asList() {
            return new PyList(iter());
        }

        @Override
        default String toStringIter() {
            return "(" + PyIterable.super.toStringIter() + ")";
        }
    }

    /**
     * represent Python {@code ()} (empty tuple).
     */
    record PyTuple0() implements PyTuple {
        @Override
        public List<PyValue> iter() {
            return List.of();
        }

        @Override
        public String toString() {
            return "()";
        }
    }

    /**
     * represent Python {@code tuple[Any]}.
     *
     * @param value value
     */
    record PyTuple1<T extends PyValue>(T value) implements PyTuple {
        @Override
        public List<PyValue> iter() {
            return List.of(value);
        }

        @Override
        public String toString() {
            return "(" + value + ",)";
        }
    }

    /**
     * represent Python {@code tuple[Any, Any]}.
     *
     * @param first  value
     * @param second value
     */
    record PyTuple2<T1 extends PyValue, T2 extends PyValue>(T1 first, T2 second) implements PyTuple {
        @Override
        public List<PyValue> iter() {
            return List.of(first, second);
        }

        @Override
        public String toString() {
            return PyTuple.super.toStringIter();
        }
    }

    /**
     * represent Python {@code tuple[Any, Any, Any]}.
     *
     * @param first  value
     * @param second value
     * @param third  value
     */
    record PyTuple3<T1 extends PyValue, T2 extends PyValue, T3 extends PyValue>(T1 first, T2 second, T3 third) implements PyTuple {
        @Override
        public List<PyValue> iter() {
            return List.of(first, second, third);
        }

        @Override
        public String toString() {
            return PyTuple.super.toStringIter();
        }
    }

    /**
     * represent Python {@code tuple[Any, ...]}.
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
    }

    /**
     * represent Python {@code list[Any]}.
     *
     * @param elements value list
     */
    record PyList(List<PyValue> elements) implements PyIterable {
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
     * represent Python {@code dict[str, Any]}.
     *
     * @param keys     key name list
     * @param elements value list.
     */
    record PyDict(List<String> keys, List<PyValue> elements) implements PyIterable {

        public PyDict {
            if (keys.size() != new HashSet<>(keys).size()) throw new RuntimeException("duplicate keys");
            if (keys.size() != elements.size()) throw new RuntimeException();
        }

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

        public int size() {
            return elements.size();
        }

        @Override
        public List<PyValue> iter() {
            return keys.stream().map(it -> (PyValue) new PyStr(it)).toList();
        }

        public List<PyTuple2<PyStr, PyValue>> items() {
            return IntStream.range(0, keys.size()).mapToObj(i -> {
                var k = keys.get(i);
                var v = elements.get(i);
                return new PyTuple2<>(new PyStr(k), v);
            }).toList();
        }

        public @Nullable PyValue get(String key) {
            var i = key.indexOf(key);
            if (i < 0) return null;
            return elements.get(i);
        }

        public PyBool asBool() {
            return size() == 0 ? PyValue.False : PyValue.True;
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
        public int length() {
            return token.length();
        }

        public PyStr asStr() {
            return new PyStr(token);
        }

        @Override
        public String toString() {
            return "token(" + token + ")";
        }
    }

    /**
     * represent Python {@code str}.
     *
     * @param string content
     */
    record PyStr(String string) implements PyValue {
        public int length() {
            return string.length();
        }

        public PyBool asBool() {
            return length() == 0 ? PyValue.False : PyValue.True;
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
