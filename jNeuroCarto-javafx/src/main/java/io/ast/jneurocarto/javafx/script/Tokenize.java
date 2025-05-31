package io.ast.jneurocarto.javafx.script;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Tokenize {

    public final String line;
    public @Nullable List<PyValue.PyParameter> values;

    public Tokenize(String line) {
        this.line = line;
    }

    public Tokenize parse() {
        var values = new ArrayList<PyValue.PyParameter>();
        var x = line.length();
        var s = nextNonSpaceChar(0, x);
        while (s < x) {
            // "..., text , ..."
            //       s  t e    x
            // "..., text "
            //       s  t e=x
            int e = nextToken(s, x);
            int t = prevNonSpaceChar(s, e - 1) + 1;
            if (s == t) {
                values.add(new PyValue.PyIndexParameter(values.size(), "", s, null));
            } else {
                parsePair(s, t, values);
            }
            s = nextNonSpaceChar(e + 1, x);
            if (e < x && s == x) { // tailing comma
                values.add(new PyValue.PyIndexParameter(values.size(), "", e + 1, null));
            }
        }
        this.values = values;
        return this;
    }

    private void parsePair(int i, int x, List<PyValue.PyParameter> values) {
        i = nextNonSpaceChar(i, x);
        int d = nextTokenUntil(i, '=', x);
        if (d < 0) {
            var p = values.size();
            var v = parseValue(i, x);
            values.add(new PyValue.PyIndexParameter(p, line.substring(i, x), i, v));
        } else {
            int k = prevNonSpaceChar(i, d - 1) + 1;
            var n = line.substring(i, k);
            d = nextNonSpaceChar(d + 1, x);
            if (d == x) {
                values.add(new PyValue.PyNamedParameter(n, line.substring(i, x), i, d, null));
            } else {
                var v = parseValue(d, x);
                values.add(new PyValue.PyNamedParameter(n, line.substring(i, x), i, d, v));
            }
        }
    }

    public int size() {
        return values == null ? 0 : values.size();
    }

    public PyValue get(int i) {
        if (values == null) throw new IndexOutOfBoundsException();
        return values.get(i);
    }

    public Stream<PyValue.PyParameter> stream() {
        return values == null ? Stream.of() : values.stream();
    }

    public <T> List<T> map(Function<PyValue, T> mapper) {
        return stream().map(mapper).toList();
    }

    int nextNonSpaceChar(int i, int x) {
        while (i < x && Character.isSpaceChar(line.charAt(i))) {
            i++;
        }
        return Math.min(i, x);
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
        } else if (isMatch(i, x, "True")) {
            return PyValue.True;
        } else if (isMatch(i, x, "False")) {
            return PyValue.False;
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
        return new PyValue.PySymbol(content);
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
        i = nextNonSpaceChar(i, x);
        int d = nextTokenUntil(i, ':', x);
        if (d < 0) {
            int k = prevNonSpaceChar(i, x - 1) + 1;
            keys.add(line.substring(i, k));
            values.add(PyValue.None);
        } else {
            int k = prevNonSpaceChar(i, d - 1) + 1;
            keys.add(line.substring(i, k));
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
