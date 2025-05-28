package io.ast.jneurocarto.core.numpy;

import java.util.Arrays;
import java.util.stream.Collectors;

public record NumpyHeader(int majorVersion, int minorVersion, String data) {
    public NumpyHeader {
        data = data.strip();
        if (!(data.startsWith("{") && data.endsWith("}"))) {
            throw new IllegalArgumentException("not a dict string : " + data);
        }
    }

    public static NumpyHeader of(int majorVersion, int minorVersion, String descr, boolean fortranOrder, int[] shape) {
        var data = "{'descr': '" +
                   descr +
                   "', 'fortran_order': " +
                   (fortranOrder ? "True" : "False") +
                   ", 'shape': (" +
                   Arrays.stream(shape).mapToObj(Integer::toString).collect(Collectors.joining(", ")) +
                   "), }";
        return new NumpyHeader(majorVersion, minorVersion, data);
    }

    public String descr() {
        var i = indexOf("descr");
        if (i < 0) throw new IllegalArgumentException("descr key not found");
        var j = data.indexOf("'", i + 1);
        if (j < 0) throw new IllegalArgumentException("descr value not found");
        var k = data.indexOf("'", j + 1);
        if (k < 0) throw new IllegalArgumentException("descr value not found");
        return data.substring(j + 1, k);
    }

    public boolean fortranOrder() {
        var i = indexOf("fortran_order");
        if (i < 0) throw new IllegalArgumentException("fortran_order key not found");
        var j = data.indexOf(",", i + 1);
        if (j < 0) throw new IllegalArgumentException("fortran_order value not found");
        return Boolean.parseBoolean(data.substring(i + 1, j).strip().toLowerCase());
    }

    public int[] shape() {
        var i = indexOf("shape");
        if (i < 0) throw new IllegalArgumentException("shape key not found");
        var j = data.indexOf("(", i + 1);
        if (j < 0) throw new IllegalArgumentException("shape value not found");
        var k = data.indexOf(")", j + 1);
        if (k < 0) throw new IllegalArgumentException("shape value not found");
        return Arrays.stream(data.substring(j + 1, k).split(", *"))
          .mapToInt(Integer::parseInt)
          .toArray();
    }

    public int ndim() {
        var i = indexOf("shape");
        if (i < 0) throw new IllegalArgumentException("shape key not found");
        var j = data.indexOf("(", i + 1);
        if (j < 0) throw new IllegalArgumentException("shape value not found");
        var k = data.indexOf(")", j + 1);
        if (k < 0) throw new IllegalArgumentException("shape value not found");
        return (int) Arrays.stream(data.substring(j + 1, k).split(", *"))
          .count();
    }

    private int indexOf(String name) {
        var key = "'" + name + "':";
        var ret = data.indexOf(key);
        if (ret < 0) return -1;
        return ret + key.length();
    }
}
