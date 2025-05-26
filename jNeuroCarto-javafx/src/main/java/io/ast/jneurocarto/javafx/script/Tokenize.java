package io.ast.jneurocarto.javafx.script;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class Tokenize {

    public final String line;
    public List<PyValue> tokens;

    public Tokenize(String line) {
        this.line = line;
    }

    public Tokenize parse() {
        tokens = parsePyIterable(0, line.length()).elements();
        return this;
    }

    public int size() {
        return tokens == null ? 0 : tokens.size();
    }

    public PyValue get(int i) {
        if (tokens == null) throw new IndexOutOfBoundsException();
        return tokens.get(i);
    }

    public Stream<PyValue> stream() {
        return tokens == null ? Stream.of() : tokens.stream();
    }

    public <T> List<T> map(Function<PyValue, T> mapper) {
        return stream().map(mapper).toList();
    }

    int nextNonSpaceChar(int i, int x) {
        while (i < x && Character.isSpaceChar(line.charAt(i))) {
            i++;
        }
        return i;
    }

    int prevNonSpaceChar(int i, int j) {
        while (i < j && Character.isSpaceChar(line.charAt(j))) {
            j--;
        }
        return j;
    }

    int nextToken(int i, int x) {
        var r = i;
        while (r < x) {
            var t = line.charAt(r);
            if (t == ',') return r;

            switch (t) {
            case '\'' -> r = nextTokenUntilEnsured(r + 1, '\'', x);
            case '"' -> r = nextTokenUntilEnsured(r + 1, '"', x);
            case '(' -> r = nextTokenUntilEnsured(r + 1, ')', x);
            case '[' -> r = nextTokenUntilEnsured(r + 1, ']', x);
            case '{' -> r = nextTokenUntilEnsured(r + 1, '}', x);
            }
            r++;
        }
        return x;
    }

    int nextTokenUntilEnsured(int i, char c, int x) {
        var r = nextTokenUntil(i, c, x);
        if (r < 0) {
            throw new RuntimeException("missing '" + c + "', matched '" + line.charAt(i) + "' at " + i);
        }
        return r;
    }

    int nextTokenUntil(int i, char c, int x) {
        var r = i;
        while (r < x) {
            var t = line.charAt(r);
            if (t == c) return r;

            switch (t) {
            case '\'' -> r = nextTokenUntilEnsured(r + 1, '\'', x);
            case '"' -> r = nextTokenUntilEnsured(r + 1, '"', x);
            case '(' -> r = nextTokenUntilEnsured(r + 1, ')', x);
            case '[' -> r = nextTokenUntilEnsured(r + 1, ']', x);
            case '{' -> r = nextTokenUntilEnsured(r + 1, '}', x);
            }
            r++;
        }
        return -1;
    }

    boolean isMatch(int i, int x, String constant) {
        var length = constant.length();
        if (i + length != x) return false;
        for (int j = 0; j < length; j++) {
            if (line.charAt(i + j) != constant.charAt(j)) return false;
        }
        return true;
    }

    public PyValue parseValue() {
        return parseValue(0, line.length());
    }

    PyValue parseValue(int i, int x) {
        if (!(i <= x)) throw new IllegalArgumentException("not a python value");
        if (isMatch(i, x, "") || isMatch(i, x, "None")) {
            return PyValue.None;
        }

        switch (line.charAt(i)) {
        case '\'', '"' -> {
            return parseStr(i, x);
        }
        case '(' -> {
            return parseTuple(i, x);
        }
        case '[' -> {
            return parseList(i, x);
        }
        case '{' -> {
            return parseDict(i, x);
        }
        }

        var content = line.substring(i, x);
        try {
            return new PyValue.PyInt(Integer.parseInt(content));
        } catch (NumberFormatException e) {
        }
        try {
            return new PyValue.PyFloat(Double.parseDouble(content));
        } catch (NumberFormatException e) {
        }
        return new PyValue.PyStr(content);
    }

    public PyValue parseTuple() {
        return parseTuple(0, line.length());
    }

    PyValue parseTuple(int i, int x) {
        if (!(i < x)) throw new IllegalArgumentException("not a python tuple");
        if (line.charAt(i) != '(') throwIAE("not a python tuple", i, x);
        if (line.charAt(x - 1) != ')') throwIAE("not a python tuple", i, x);
        if (line.charAt(i + 1) == ',') throwIAE("not a python tuple", i, x);
        var j = removeTailingComma(i + 1, x - 1);
        var ret = parsePyIterable(i + 1, j).asTuple();

        if (ret instanceof PyValue.PyTuple1(var value)) {
            var k = nextNonSpaceChar(j, x);
            if (k < x && line.charAt(k) != ',') return value;
        }

        return ret;
    }

    public PyValue.PyList parseList() {
        return parseList(0, line.length());
    }

    PyValue.PyList parseList(int i, int x) {
        if (!(i < x)) throw new IllegalArgumentException("not a python list");
        if (line.charAt(i) != '[') throwIAE("not a python list", i, x);
        if (line.charAt(x - 1) != ']') throwIAE("not a python list", i, x);
        if (line.charAt(i + 1) == ',') throwIAE("not a python list", i, x);
        var j = removeTailingComma(i + 1, x - 1);
        return parsePyIterable(i + 1, j);
    }

    private int removeTailingComma(int i, int x) {
        int j = prevNonSpaceChar(i, x - 1);
        if (j == i) return i;
        return line.charAt(j) != ',' ? j + 1 : prevNonSpaceChar(i, j - 1) + 1;
    }

    private PyValue.PyList parsePyIterable(int i, int x) {
        var elements = new ArrayList<PyValue>();
        var s = nextNonSpaceChar(i, x);
        while (s < x) {
            int e = nextToken(s, x);
            int t = prevNonSpaceChar(s, e - 1) + 1;
            elements.add(parseValue(s, t));
            s = nextNonSpaceChar(e + 1, x);
        }
        if (elements.isEmpty()) return PyValue.EMPTY_LIST;
        return new PyValue.PyList(elements);
    }

    public PyValue.PyDict parseDict() {
        return parseDict(0, line.length());
    }

    PyValue.PyDict parseDict(int i, int x) {
        if (!(i < x)) throw new IllegalArgumentException("not a python dict");
        if (line.charAt(i) != '{') throwIAE("not a python dict", i, x);
        if (line.charAt(x - 1) != '}') throwIAE("not a python dict", i, x);
        if (line.charAt(i + 1) == ',') throwIAE("not a python dict", i, x);

        var keys = new ArrayList<String>();
        var values = new ArrayList<PyValue>();

        var s = nextNonSpaceChar(i + 1, x - 1);
        while (s < x - 1) {
            int e = nextToken(s, x - 1);
            int t = prevNonSpaceChar(s, e - 1) + 1;
            parseDictEntry(s, t, keys, values);
            s = nextNonSpaceChar(e + 1, x - 1);
        }

        if (keys.isEmpty()) return PyValue.EMPTY_DICT;
        return new PyValue.PyDict(keys, values);
    }

    private void parseDictEntry(int i, int x, List<String> keys, List<PyValue> values) {
        int k1 = nextNonSpaceChar(i, x);
        int d = nextTokenUntil(k1, ':', x);
        if (d < 0) {
            int k2 = prevNonSpaceChar(k1, x - 1) + 1;
            keys.add(line.substring(k1, k2));
            values.add(PyValue.None);
        } else {
            int k2 = prevNonSpaceChar(k1, d - 1) + 1;
            keys.add(line.substring(k1, k2));
            d = nextNonSpaceChar(d + 1, x);
            values.add(parseValue(d, x));
        }
    }

    public PyValue.PyStr parseStr() {
        return parseStr(0, line.length());
    }

    PyValue.PyStr parseStr(int i, int x) {
        if (!(i < x)) throw new IllegalArgumentException("not a python str");
        var t = line.charAt(i);
        if (t != '\'' && t != '"') throwIAE("not a python str", i, x);
        if (line.charAt(x - 1) != t) throwIAE("not a python str", i, x);
        return new PyValue.PyStr(line.substring(i + 1, x - 1));
    }

    private void throwIAE(String message, int i, int x) {
        throw new IllegalArgumentException(message + " : " + line.substring(i, x));
    }
}
