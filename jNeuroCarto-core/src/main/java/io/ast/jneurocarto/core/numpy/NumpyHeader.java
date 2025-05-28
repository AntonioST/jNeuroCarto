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

    public static int ndim(int[] shape) {
        return shape.length;
    }

    public long size() {
        return size(shape());
    }

    public static long size(int[] shape) {
        long ret = 1;
        for (var i : shape) {
            ret *= i;
        }
        return ret;
    }

    public boolean match(int[] output) {
        return match(output, shape());
    }

    public static boolean match(int[] output, int[] shape) {
        if (numberOfNegDim(shape) > 0) {
            throw new IllegalArgumentException("shape does not have determined dimension : " + Arrays.toString(shape));
        }

        if (output.length == 0) {
            return shape.length == 0;
        } else if (shape.length == 0) {
            return false;
        }

//        assert shape.length > 0;
//        assert output.length > 0;

        int numberOfNegDim = numberOfNegDim(output);

        if (numberOfNegDim == 0) {
            var shapeSize = size(shape);
            var outputSize = size(output);
            return shapeSize >= outputSize && shapeSize % outputSize == 0;
        } else if (numberOfNegDim == 1) {
            var i = indexOfNegDim(output);
            output[i] = -1;
            var shapeSize = size(shape);
            var outputSize = -size(output);
            if (outputSize > shapeSize) {
                return false;
            }
            if (shapeSize % outputSize != 0) {
                return false;
            }
            output[i] = (int) (shapeSize / outputSize);
            return true;
        } else {
            throw new RuntimeException("too many undetermined dimension : " + Arrays.toString(output));
        }
    }

    private static int numberOfNegDim(int[] shape) {
        int numberOfNegDim = 0;
        for (var o : shape) {
            if (o < 0) numberOfNegDim++;
        }
        return numberOfNegDim;
    }

    private static int indexOfNegDim(int[] shape) {
        for (int i = 0, length = shape.length; i < length; i++) {
            if (shape[i] < 0) return i;
        }
        return -1;
    }

    private int indexOf(String name) {
        var key = "'" + name + "':";
        var ret = data.indexOf(key);
        if (ret < 0) return -1;
        return ret + key.length();
    }
}
