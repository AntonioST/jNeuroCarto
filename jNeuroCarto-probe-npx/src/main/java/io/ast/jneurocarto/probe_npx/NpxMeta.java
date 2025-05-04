package io.ast.jneurocarto.probe_npx;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NpxMeta {
    private final Map<String, String> meta;

    public NpxMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public @Nullable String serialNumber() {
        return meta.get("imDatPrb_sn");
    }

    public @Nullable String imroTable() {
        return meta.get("~imroTbl");
    }
}
