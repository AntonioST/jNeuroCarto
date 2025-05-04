package io.ast.jneurocarto.probe_npx.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.NpxMeta;

public final class Meta {
    private Meta() {
        throw new RuntimeException();
    }

    public static ChannelMap read(Path file) throws IOException {
        if (!file.getFileName().toString().endsWith(".meta")) {
            throw new IOException();
        }

        var content = new HashMap<String, String>();
        try (var reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int i = line.indexOf('=');
                content.put(line.substring(0, i), line.substring(i + 1));
            }
        }

        var meta = new NpxMeta(content);
        var ret = Imro.read(meta.imroTable());
        ret.setMeta(meta);
        return ret;
    }
}
