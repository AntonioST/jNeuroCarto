package io.ast.jneurocarto.probe_npx.io;

import java.io.PrintStream;

import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

public abstract class ImroIO {

    public final NpxProbeType type;
    protected int reference = 0;

    protected ImroIO(NpxProbeType type) {
        this.type = type;
    }

    public static ImroIO of(NpxProbeType type) {
        return switch (type) {
            default -> new ImroNp1(type);
        };
    }

    public abstract void parseHeader(int[] headers);

    public abstract Electrode parseElectrodes(int[] args);

    public void stringHeader(PrintStream out, ChannelMap map) {
        if (map.type() != type) throw new IllegalArgumentException();
        out.printf("(%d,%d)", type.code(), map.nChannel());
        reference = map.getReference();
    }

    public abstract void stringElectrode(PrintStream out, int channel, Electrode e);
}
