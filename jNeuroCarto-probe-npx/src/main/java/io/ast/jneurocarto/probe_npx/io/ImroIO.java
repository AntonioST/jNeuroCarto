package io.ast.jneurocarto.probe_npx.io;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@NullMarked
public abstract class ImroIO {

    public final NpxProbeType type;
    protected @Nullable List<Electrode> electrodes;
    protected int reference = 0;

    protected ImroIO(NpxProbeType type) {
        this.type = type;
    }

    public static ImroIO of(NpxProbeType type) {
        return switch (type) {
            case NpxProbeType.NP21Base base -> new ImroNp21(base);
            case NpxProbeType.NP24Base base -> new ImroNp24(base);
            case NpxProbeType.NP1110 _ -> new ImroNp1110();
            case NpxProbeType.NP2020 _ -> new ImroNp2020();
            case NpxProbeType.NP3010 _ -> new ImroNp3010();
            case NpxProbeType.NP3020 _ -> new ImroNp3020();
            default -> new ImroNp1(type);
        };
    }

    public void parseHeader(int[] headers) {
        if (headers[0] != type.code()) throw new IllegalArgumentException();
        var nChannel = headers[1];
        electrodes = new ArrayList<>(nChannel);
    }

    public abstract boolean parseElectrodes(int[] args);

    protected boolean addElectrode(Electrode e) {
        if (electrodes == null) throw new IllegalStateException();
        electrodes.add(e);
        return electrodes.size() < type.nChannel();
    }

    public ChannelMap newChannelmap() {
        if (electrodes == null) throw new IllegalStateException();
        if (electrodes.size() != type.nChannel()) throw new IllegalArgumentException();
        var ret = new ChannelMap(type, electrodes, null);
        ret.setReference(reference);
        return ret;
    }

    public void stringHeader(PrintStream out, ChannelMap map) {
        if (map.type() != type) throw new IllegalArgumentException();
        out.printf("(%d,%d)", type.code(), map.nChannel());
        reference = map.getReference();
    }

    public abstract void stringElectrode(PrintStream out, int channel, Electrode e);

    public interface RestrictedGainValue {
        int[] apGain();

        int[] lfGain();


        default int checkApGainValue(int value) {
            var gain = apGain();
            for (int j : gain) {
                if (value == j) return value;
            }
            throw new IllegalArgumentException("unallowed apGain value: " + value);
        }

        default int checkApGainValue(int value, int defaultGain) {
            var gain = apGain();
            for (int j : gain) {
                if (value == j) return value;
            }
            return checkApGainValue(defaultGain);
        }

        default int checkLfGainValue(int value) {
            var gain = lfGain();
            for (int j : gain) {
                if (value == j) return value;
            }
            throw new IllegalArgumentException("unallowed lfGain value: " + value);
        }

        default int checkLfGainValue(int value, int defaultGain) {
            var gain = lfGain();
            for (int j : gain) {
                if (value == j) return value;
            }
            return checkLfGainValue(defaultGain);
        }
    }
}
