package io.ast.jneurocarto.probe_npx.io;

import java.io.PrintStream;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@NullMarked
class ImroNp2020 extends ImroIO {
    protected ImroNp2020() {
        super(NpxProbeType.np2020);
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T2020.cppL#35)
    @Override
    public boolean parseElectrodes(int[] args) {
        assert args.length == 5;
        var channel = args[0];
        var shank = args[1];
        var bank = args[2];
        reference = args[3];
        var ec = args[4];
        ec += bank * 384;
        assert new ChannelMapUtil.CB(channel, bank).equals(ChannelMapUtil.e2c2020(shank, ec));

        var cr = ChannelMapUtil.e2cr(type, ec);
        return addElectrode(new Electrode(shank, cr.c(), cr.r()));
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T2020.cpp#L22)
    @Override
    public void stringElectrode(PrintStream out, int channel, Electrode e) {
        var electrode = ChannelMapUtil.cr2e(type, e);
        var cb = ChannelMapUtil.e2c2020(e.shank, electrode);
        assert cb.channel() == channel;
        out.printf("(%d %d %d %d %d)", channel, e.shank, cb.bank(), reference, electrode % 384);
    }
}
