package io.ast.jneurocarto.probe_npx.io;

import java.io.PrintStream;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@NullMarked
class ImroNp21 extends ImroIO {
    protected ImroNp21(NpxProbeType.NP21Base type) {
        super(type);
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T21base.cpp#L80)
    @Override
    public boolean parseElectrodes(int[] args) {
        assert args.length == 4;
        var channel = args[0];
        var bank = args[1];
        reference = args[2];
        var ec = args[3];
        bank = switch (Integer.lowestOneBit(bank)) {
            case 1 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            default -> 3;
        };
        assert new ChannelMapUtil.CB(channel, bank).equals(ChannelMapUtil.e2c21(ec));

        var cr = ChannelMapUtil.e2cr(type, ec);
        return addElectrode(new Electrode(0, cr.c(), cr.r()));
    }

    @Override
    public void stringElectrode(PrintStream out, int channel, Electrode e) {
        var electrode = ChannelMapUtil.cr2e(type, e);
        var cb = ChannelMapUtil.e2c21(electrode);
        assert cb.channel() == channel;
        var bank = 1 << cb.bank();
        out.printf("(%d %d %d %d)", channel, bank, reference, electrode);
    }
}
