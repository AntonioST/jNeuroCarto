package io.ast.jneurocarto.javafx.script;


import java.util.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface PyValue {

    PyNone None = new PyNone();
    PyList EMPTY_LIST = new PyList(List.of());
    PyTuple0 EMPTY_TUPLE = new PyTuple0();
    PyDict EMPTY_DICT = new PyDict(List.of(), List.of());

    record PyNone() implements PyValue {
    }

    record PyInt(int value) implements PyValue {
    }

    record PyFloat(double value) implements PyValue {
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
    }

    sealed interface PyTuple extends PyIterable {
        @Override
        default String rawTypeStr() {
            return "tuple";
        }

        default PyList asList() {
            return new PyList(elements());
        }
    }

    record PyTuple0() implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of();
        }
    }

    record PyTuple1(PyValue value) implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of(value);
        }
    }

    record PyTuple2(PyValue first, PyValue second) implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of(first, second);
        }
    }

    record PyTuple3(PyValue first, PyValue second, PyValue third) implements PyTuple {
        @Override
        public List<PyValue> elements() {
            return List.of(first, second, third);
        }
    }

    record PyTupleN(List<PyValue> elements) implements PyTuple {
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

    }

    record PyStr(String string) implements PyValue {
        public int length() {
            return string.length();
        }
    }

    sealed interface PyParameter extends PyValue {
    }

    record PyIndexParameter(int index, @Nullable PyValue value) implements PyParameter {
    }

    record PyNamedParameter(String name, @Nullable PyValue value) implements PyParameter {
    }


}
