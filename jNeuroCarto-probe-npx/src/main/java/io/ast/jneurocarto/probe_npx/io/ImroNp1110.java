package io.ast.jneurocarto.probe_npx.io;

import java.io.PrintStream;
import java.util.ArrayList;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@NullMarked
class ImroNp1110 extends ImroIO {

    public static final int INNER = 0;
    public static final int OUTER = 1;
    public static final int ALL = 2;

    private int ap = 500;
    private int lf = 250;
    private int ft = 1;
    private int mode = ALL;

    protected ImroNp1110() {
        super(NpxProbeType.np1110);
    }

    @Override
    public void parseHeader(int[] headers) {
        assert headers.length == 6;
        if (headers[0] != type.code()) throw new IllegalArgumentException();
        var mode = headers[1];
        if (mode < 0 || mode > ALL) throw new IllegalArgumentException();
        reference = headers[2];
        this.ap = headers[3];
        this.lf = headers[4];
        this.ft = headers[5];
        this.mode = mode;
        electrodes = new ArrayList<>();
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1110.cpp#L170)
    private static boolean isBankCrossed(int bankA, int bankB) {
        var a = (bankA / 4) % 2;
        var b = (bankB / 4) % 2;
        return a != b;
    }

    private int bank(int channel, int bankA, int bankB) {
        return switch (mode) {
            case ALL -> {
                if (bankA != bankB)
                    throw new IllegalArgumentException("In All col mode must have same bankA and bankB value");
                yield bankA;
            }
            case OUTER -> {
                if (!isBankCrossed(bankA, bankB))
                    throw new IllegalArgumentException("In OUTER mode, only either bankA or bankB is col-crossed exactly");
                yield switch (ChannelMapUtil.np1110Col(channel, bankA)) {
                    case 0, 2, 5, 7 -> bankA;
                    default -> bankB;
                };
            }
            case INNER -> {
                if (!isBankCrossed(bankA, bankB))
                    throw new IllegalArgumentException("In INNER mode, only either bankA or bankB is col-crossed exactly");
                yield switch (ChannelMapUtil.np1110Col(channel, bankA)) {
                    case 1, 3, 4, 6 -> bankA;
                    default -> bankB;
                };
            }
            default -> throw new IllegalArgumentException("illegal mode");
        };
    }

    @Override
    public boolean parseElectrodes(int[] args) {
        assert args.length == 3;
        var channel = args[0];
        var bankA = args[1];
        var bankB = args[2];
        var bank = bank(channel, bankA, bankB);
        var group = ChannelMapUtil.np1110Group(channel);
        var col = ChannelMapUtil.np1110Col(channel, bank, group);
        var row = ChannelMapUtil.np1110Row(channel, bank, group);
        var e = new Electrode(0, col, row);
        e.apBandGain = ap;
        e.lfBandBain = lf;
        e.apHpFilter = ft != 0;
        e.bankA = bankA;
        e.bankB = bankB;
        return addElectrode(e);
    }

    @Override
    public ChannelMap newChannelmap() {
        var ret = super.newChannelmap();
        ret.mode = switch (mode) {
            case INNER -> NpxProbeType.NP1110.Mode.INNER;
            case OUTER -> NpxProbeType.NP1110.Mode.OUTER;
            case ALL -> NpxProbeType.NP1110.Mode.ALL;
            default -> throw new IllegalArgumentException();
        };
        return ret;
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1110.cpp#L19)
    @Override
    public void stringHeader(PrintStream out, ChannelMap map) {
        if (map.type() != type) throw new IllegalArgumentException();
        var e = map.getChannel(0);
        out.printf("(%d,%d,%d,%d,%d,%d)",
          type.code(),
          mode,
          reference,
          e.apBandGain,
          e.lfBandBain,
          e.apHpFilter ? 1 : 0);
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1110.cpp#L32)
    @Override
    public void stringElectrode(PrintStream out, int channel, Electrode e) {
        out.printf("(%d %d %d)", channel, e.bankA, e.bankB);
    }
}
