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

    record PyNone() implements PyValue {
        @Override
        public String toString() {
            return "None";
        }
    }

    record PyBool(boolean value) implements PyValue {
        @Override
        public String toString() {
            return "bool(" + value + ")";
        }
    }

    record PyInt(int value) implements PyValue {
        public PyBool asBool() {
            return value == 0 ? PyValue.False : PyValue.True;
        }

        @Override
        public String toString() {
            return "int(" + value + ")";
        }
    }

    record PyFloat(double value) implements PyValue {
        @Override
        public String toString() {
            return "float(" + value + ")";
        }
    }

    sealed interface PyIterable extends PyValue {
        String rawTypeStr();

        List<PyValue> elements();

        default int size() {
            return elements().size();
        }

        default PyValue get(int i) {
            return elements().get(i);
        }

        default PyBool asBool() {
            return size() == 0 ? PyValue.False : PyValue.True;
        }

        default int[] toIntArray() {
            var elements = elements();
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
            var elements = elements();
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
            var elements = elements();
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
            return elements().stream().map(PyValue::toString).collect(Collectors.joining(", "));
        }
    }

    sealed interface PyTuple extends PyIterable {
        @Override
        default String rawTypeStr() {
            return "tuple";
        }

        default PyList asList() {
            return new PyList(elements());
        }

        @Override
        default String toStringIter() {
            return "(" + PyIterable.super.toStringIter() + ")";
        }
    }

    record PyTuple0() implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of();
        }

        @Override
        public String toString() {
            return "()";
        }
    }

    record PyTuple1(PyValue value) implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of(value);
        }

        @Override
        public String toString() {
            return "(" + value + ",)";
        }
    }

    record PyTuple2(PyValue first, PyValue second) implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of(first, second);
        }

        @Override
        public String toString() {
            return PyTuple.super.toStringIter();
        }
    }

    record PyTuple3(PyValue first, PyValue second, PyValue third) implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of(first, second, third);
        }

        @Override
        public String toString() {
            return PyTuple.super.toStringIter();
        }
    }

    record PyTupleN(List<PyValue> elements) implements PyTuple {
        @Override
        public String toString() {
            return PyTuple.super.toStringIter();
        }
    }

    record PyList(List<PyValue> elements) implements PyIterable {
        public PyList(PyValue... elements) {
            this(Arrays.asList(elements));
        }

        @Override
        public String rawTypeStr() {
            return "list";
        }

        public PyTuple asTuple() {
            return switch (elements.size()) {
                case 0 -> PyValue.EMPTY_TUPLE;
                case 1 -> new PyTuple1(elements.get(0));
                case 2 -> new PyTuple2(elements.get(0), elements.get(1));
                case 3 -> new PyTuple3(elements.get(0), elements.get(1), elements.get(2));
                default -> new PyTupleN(elements);
            };
        }

        @Override
        public String toString() {
            return "[" + PyIterable.super.toStringIter() + "]";
        }
    }

    record PyDict(List<String> keys, List<PyValue> elements) implements PyValue {

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

        public int size() {
            return elements.size();
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

    record PySymbol(String symbol) implements PyValue {
        public int length() {
            return symbol.length();
        }

        public PyStr asStr() {
            return new PyStr(symbol);
        }

        @Override
        public String toString() {
            return "symbol(" + symbol + ")";
        }
    }

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

    sealed interface PyParameter extends PyValue {
        String text();

        int offset();

        @Nullable
        PyValue value();

        String valueText();
    }

    record PyIndexParameter(int index, String text, int offset, @Nullable PyValue value) implements PyParameter {
        public String valueText() {
            return text;
        }

        @Override
        public String toString() {
            return index + "=" + value;
        }
    }

    record PyNamedParameter(String name, String text, int offset, int delimiter, @Nullable PyValue value) implements PyParameter {
        public String valueText() {
            return text.substring(delimiter - offset);
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }


}
