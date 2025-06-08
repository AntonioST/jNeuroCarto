package io.ast.jneurocarto.core.numpy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@NullMarked
public class NpzFile implements AutoCloseable {
    private @Nullable
    final Path file;
    private @Nullable ZipInputStream zin;
    private @Nullable ZipOutputStream zout;

    public NpzFile(Path file) {
        this.file = file;
    }

    private NpzFile() {
        this.file = null;
    }

    public static NpzFile open(ZipInputStream zin) {
        var ret = new NpzFile();
        ret.zin = zin;
        return ret;
    }

    public static NpzFile open(ZipOutputStream zout) {
        var ret = new NpzFile();
        ret.zout = zout;
        return ret;
    }

    private ZipInputStream ensureOpenRead() throws IOException {
        if (zin != null) return zin;
        if (zout != null) throw new RuntimeException("during writing");
        if (file == null) throw new RuntimeException("missing file");

        zin = new ZipInputStream(new BufferedInputStream(Files.newInputStream(file)));
        return zin;
    }

    private ZipOutputStream ensureOpenWrite() throws IOException {
        if (zout != null) return zout;
        if (zin != null) throw new RuntimeException("during reading");
        if (file == null) throw new RuntimeException("missing file");

        zout = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(file, CREATE, TRUNCATE_EXISTING)));
        return zout;
    }

    @Override
    public void close() throws Exception {
        if (zin != null) zin.close();
        if (zout != null) zout.close();
        zin = null;
        zout = null;
    }

    public Set<String> keys() throws IOException {
        try (var zin = ensureOpenRead()) {
            return keys(zin);
        }
    }

    public static Set<String> keys(ZipInputStream zin) throws IOException {
        var ret = new HashSet<String>();
        ZipEntry e;
        while ((e = zin.getNextEntry()) != null) {
            if (!e.isDirectory()) {
                var name = e.getName();
                if (name.endsWith(".npy")) {
                    ret.add(name.substring(0, name.length() - 4));
                }
            }
        }
        return ret;
    }

    /*=====*
     * get *
     *=====*/

    public <T> @Nullable T get(String key, ValueArray<T> of) throws IOException {
        if (file == null) throw new RuntimeException("missing file");
        try (var zin = ensureOpenRead()) {
            var e = get(zin, key);
            if (e == null) return null;
            return Numpy.read(zin, of);
        }
    }

    public Numpy.@Nullable Read get(String key, Numpy.CheckNumberHeader of) throws IOException {
        if (file == null) throw new RuntimeException("missing file");
        try (var zin = ensureOpenRead()) {
            var e = get(zin, key);
            if (e == null) return null;
            return Numpy.read(zin, of);
        }
    }

    private static @Nullable ZipEntry get(ZipInputStream zin, String key) throws IOException {
        var name = key + ".npy";

        ZipEntry e;
        while ((e = zin.getNextEntry()) != null) {
            if (!e.isDirectory() && e.getName().equals(name)) return e;
        }
        return null;
    }


    public <T> Map<String, T> get(Predicate<String> keySelector, ValueArray<T> of) throws IOException {
        if (file == null) throw new RuntimeException("missing file");

        var ret = new HashMap<String, T>();
        try (var zin = ensureOpenRead()) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                var name = e.getName().replace(".npy", "");
                if (!e.isDirectory() && keySelector.test(name)) {
                    ret.put(name, Numpy.read(zin, of));
                }
            }
        }
        return ret;
    }


    public <T> Map<String, T> get(Predicate<String> keySelector, ValueArray<T> of, BiFunction<String, RuntimeException, T> onError) throws IOException {
        if (file == null) throw new RuntimeException("missing file");

        var ret = new HashMap<String, T>();
        try (var zin = ensureOpenRead()) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                var name = e.getName().replace(".npy", "");
                if (!e.isDirectory() && keySelector.test(name)) {
                    T read;

                    try {
                        read = Numpy.read(zin, of);
                    } catch (RuntimeException ex) {
                        read = onError.apply(name, ex);
                    }

                    ret.put(name, read);
                }
            }
        }
        return ret;
    }

    public Map<String, Numpy.Read> get(Predicate<String> keySelector, Numpy.CheckNumberHeader of) throws IOException {
        if (file == null) throw new RuntimeException("missing file");

        var ret = new HashMap<String, Numpy.Read>();
        try (var zin = ensureOpenRead()) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                var name = e.getName().replace(".npy", "");
                if (!e.isDirectory() && keySelector.test(name)) {
                    ret.put(name, Numpy.read(zin, of));
                }
            }
        }
        return ret;
    }

    public Map<String, Numpy.Read> get(Predicate<String> keySelector,
                                       Numpy.CheckNumberHeader of,
                                       BiFunction<String, RuntimeException, Object> onError) throws IOException {
        if (file == null) throw new RuntimeException("missing file");

        class Wrapper implements Numpy.CheckNumberHeader {
            @Nullable
            NumpyHeader header;

            @Override
            public ValueArray<?> check(@NonNull NumpyHeader header) {
                this.header = header;
                return of.check(header);
            }
        }

        var oc = new Wrapper();
        var ret = new HashMap<String, Numpy.Read>();

        try (var zin = ensureOpenRead()) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                var name = e.getName().replace(".npy", "");
                if (!e.isDirectory() && keySelector.test(name)) {
                    Numpy.Read read;

                    oc.header = null;
                    try {
                        read = Numpy.read(zin, oc);
                    } catch (RuntimeException ex) {
                        var obj = onError.apply(name, ex);
                        read = new Numpy.Read(oc.header, obj);
                    }
                    ret.put(name, read);
                }
            }
        }
        return ret;
    }

    /*======*
     * read *
     *======*/

    public @Nullable String getNextArray() throws IOException {
        var zin = ensureOpenRead();
        ZipEntry e;
        while ((e = zin.getNextEntry()) != null) {
            if (!e.isDirectory()) {
                var name = e.getName();
                if (name.endsWith(".npy")) {
                    return name.substring(0, name.length() - 4);
                }
            }
        }
        return null;
    }

    public <T> T read(ValueArray<T> of) throws IOException {
        ZipInputStream zin1 = ensureOpenRead();
        return Numpy.read(zin1, of);
    }

    public Numpy.Read read(Numpy.CheckNumberHeader of) throws IOException {
        ZipInputStream zin1 = ensureOpenRead();
        return Numpy.read(zin1, of);
    }

    /*=====*
     * put *
     *=====*/

    /**
     * write numpy array to stream.
     *
     * @param key   name of the array
     * @param array data array
     * @throws IOException when any IO error
     */
    public void put(String key, int[] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfInt());
    }

    public void put(String key, int[][] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfD2Int());
    }

    public void put(String key, int[][][] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfD3Int());
    }

    public void put(String key, double[] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfDouble());
    }

    public void put(String key, double[][] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfD2Double());
    }

    public void put(String key, double[][][] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfD3Double());
    }

    public void put(String key, boolean[] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfBoolean());
    }

    public void put(String key, boolean[][] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfD2Boolean());
    }

    public void put(String key, boolean[][][] array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfD3Boolean());
    }

    public void put(String key, FlatIntArray array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfFlatInt());
    }

    public void put(String key, FlatDoubleArray array) throws IOException {
        put(ensureOpenWrite(), key, array, new OfFlatDouble());
    }

    public <T> void put(String key, T data, ValueArray<T> of) throws IOException {
        put(ensureOpenWrite(), key, data, of);
    }

    public <T> void put(Map<String, T> data, ValueArray<T> of) throws IOException {
        var zout = ensureOpenWrite();
        for (var e : data.entrySet()) {
            put(zout, e.getKey(), e.getValue(), of);
        }
    }

    private <T> void put(ZipOutputStream zout, String key, T data, ValueArray<T> of) throws IOException {
        var e = new ZipEntry(key + ".npy");
        zout.putNextEntry(e);
        Numpy.write(zout, data, of);
    }


    /*===========*
     * test main *
     *===========*/

    public static void main(String[] args) throws Exception {
        try (var file = new NpzFile(Path.of(args[0]))) {
            if (args.length == 1) {
                file.keys().stream()
                    .sorted()
                    .forEach(System.out::println);
            } else {
                for (int i = 1, length = args.length; i < length; i++) {
                    var header = file.get(args[i], Numpy.ofHeader());
                    System.out.println(args[i] + " shape " + Arrays.toString(header.shape()));
                }
            }
        }
    }

}
