package io.ast.jneurocarto.probe_npx.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

import static java.nio.file.StandardOpenOption.*;

public final class Imro {
    private Imro() {
        throw new RuntimeException();
    }

    public static ChannelMap read(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return read(reader);
        }
    }

    public static ChannelMap read(InputStream stream) throws IOException {
        return read(new BufferedReader(new InputStreamReader(stream)));
    }

    public static ChannelMap read(BufferedReader reader) throws IOException {
        return read(reader.readLine());
    }

    public static ChannelMap read(String source) throws IOException {
        source = source.strip();
        if (!source.startsWith("(") || !source.endsWith(")")) {
            throw new IOException("not imro format");
        }

        ImroIO io = null;

        var iter = new TokenIterator(source);
        while (iter.hasNext()) {
            var parts = iter.next();
            if (io == null) {
                io = ImroIO.of(NpxProbeType.of(parts[0]));
                io.parseHeader(parts);
            } else if (!io.parseElectrodes(parts)) {
                break;
            }
        }

        if (io == null) {
            throw new IOException("not imro format");
        }

        return io.newChannelmap();
    }

    public static String stringify(ChannelMap chmap) {
        var builder = new StringBuilder();
        write(builder, chmap);
        return builder.toString();
    }

    public static void write(Path file, ChannelMap chmap) throws IOException {
        if (chmap.size() != chmap.nChannel()) {
            throw new RuntimeException("incomplete chmap.");
        }

        try (var os = Files.newOutputStream(file, CREATE, TRUNCATE_EXISTING, WRITE);
             var out = new PrintStream(new BufferedOutputStream(os))) {
            write(out, chmap);
        }
    }

    public static void write(StringBuilder out, ChannelMap chmap) {
        try {
            write(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                    out.append((char) b);
                }
            }), chmap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(PrintStream out, ChannelMap chmap) throws IOException {
        if (chmap.size() != chmap.nChannel()) {
            throw new RuntimeException("incomplete chmap");
        }

        var io = ImroIO.of(chmap.type());

        // header
        io.stringHeader(out, chmap);

        for (int i = 0, n = chmap.nChannel(); i < n; i++) {
            var electrode = chmap.getChannel(i);
            assert electrode != null;
            io.stringElectrode(out, i, electrode);
        }
    }

    private static class TokenIterator implements Iterator<int[]> {

        private final String source;
        private int i = -1; // left '('
        private int j; // right ')'
        private int[] result;

        TokenIterator(String source) throws IOException {
            source = source.strip();
            if (!source.startsWith("(") || !source.endsWith(")")) {
                throw new IOException("not imro format");
            }

            this.source = source;
        }

        private void init() {
            i = 0;
            j = source.indexOf(')', i);
            if (i < j) {
                var content = source.substring(i + 1, j);
                result = toInt(content.split(","));
            } else {
                result = null;
            }
        }

        private int[] toInt(String[] parts) {
            var ret = new int[parts.length];
            for (int i = 0, length = parts.length; i < length; i++) {
                ret[i] = Integer.parseInt(parts[i]);
            }
            return ret;
        }

        @Override
        public boolean hasNext() {
            if (i < 0) {
                init();
            }
            return j > i;
        }


        @Override
        public int[] next() {
            if (i < 0) {
                init();
            }

            if (i < j) {
                var ret = result;
                i = j + 1;
                if (i < source.length()) {
                    if (source.charAt(i) != '(') {
                        throw new RuntimeException();
                    }
                    j = source.indexOf(')', i);
                    if (j > i) {
                        var content = source.substring(i + 1, j);
                        result = toInt(content.split(" "));
                    }
                } else {
                    j = -1;
                }

                return ret;
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
