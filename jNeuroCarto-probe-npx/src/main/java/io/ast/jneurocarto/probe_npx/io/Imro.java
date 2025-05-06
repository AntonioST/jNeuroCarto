package io.ast.jneurocarto.probe_npx.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.Electrode;
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

        NpxProbeType code = null;
        int ref = 0;
        var electrodes = new ArrayList<Electrode>();

        var iter = new TokenIterator(source);
        while (iter.hasNext()) {
            var part = iter.next();
            switch (code) {
            case null:
                code = NpxProbeType.of(part[0]);
                break;
            case NpxProbeType.NP1 _: {
                var channel = part[0];
                var bank = part[1];
                ref = part[2];
                var ap = part[3];
                var lf = part[4];
                var ft = part[4];
                var cr = ChannelMapUtil.e2cr(code, channel);
                var e = new Electrode(0, cr.c(), cr.r());
                e.apBandGain = ap;
                e.lfBandBain = lf;
                e.apHpFilter = ft != 0;
                electrodes.add(e);
                break;
            }
            case NpxProbeType.NP21 _: {
                var channel = part[0];
                var bank = part[1];
                ref = part[2];
                var ec = part[3];
                var cb = ChannelMapUtil.e2cb(code, ec);
                assert cb.channel() == channel && cb.bank() == bank;
                var cr = ChannelMapUtil.e2cr(code, ec);
                electrodes.add(new Electrode(0, cr.c(), cr.r()));
                break;
            }
            case NpxProbeType.NP24 _: {
                var channel = part[0];
                var shank = part[1];
                var bank = part[2];
                ref = part[3];
                var ec = part[4];
                var cb = ChannelMapUtil.e2cb(code, ec);
                assert cb.channel() == channel && cb.bank() == bank;
                var cr = ChannelMapUtil.e2cr(code, ec);
                electrodes.add(new Electrode(shank, cr.c(), cr.r()));
                break;
            }
            }
        }

        if (code == null) {
            throw new IOException("not imro format");
        }

        var ret = new ChannelMap(code, electrodes, null);
        ret.setReference(ref);
        return ret;
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

    public static void write(StringBuilder out, ChannelMap chmap) throws IOException {
        write(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                out.append((char) b);
            }
        }), chmap);
    }

    public static void write(PrintStream out, ChannelMap chmap) throws IOException {
        if (chmap.size() != chmap.nChannel()) {
            throw new RuntimeException("incomplete chmap");
        }

        // header
        out.printf("(%d,%d)", chmap.type().code(), chmap.nChannel());

        // channels
        var reference = chmap.getReference();
        var type = chmap.type();
        switch (type) {
        case NpxProbeType.NP1 _:
            for (int i = 0, n = chmap.nChannel(); i < n; i++) {
                var electrode = chmap.getChannel(i);
                assert electrode != null;
                out.printf("(%d 0 %d %d %d %d)", i, reference, electrode.apBandGain, electrode.lfBandBain, electrode.apHpFilter ? 1 : 0);
            }
            break;
        case NpxProbeType.NP21 _:
            for (var electrode : chmap) {
                assert electrode != null;
                var e = ChannelMapUtil.cr2e(type, electrode);
                var cb = ChannelMapUtil.e2c21(e);
                out.printf("(%d %d %d %d)", cb.channel(), cb.bank(), reference, e);
            }
            break;
        case NpxProbeType.NP24 _:
            for (var electrode : chmap) {
                assert electrode != null;
                var e = ChannelMapUtil.cr2e(type, electrode);
                var cb = ChannelMapUtil.e2c24(electrode.shank, e);
                out.printf("(%d %d %d %d %d)", cb.channel(), electrode.shank, cb.bank(), reference, e);
            }
            break;
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
