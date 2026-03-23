package io.ast.jneurocarto.probe_npx.io;

import java.io.PrintStream;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@NullMarked
class ImroNp3010 extends ImroIO {
    protected ImroNp3010() {
        super(NpxProbeType.np3010);
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T3010base.cpp#L32)
    @Override
    public boolean parseElectrodes(int[] args) {
        assert args.length == 4;
        var channel = args[0];
        var bank = args[1];
        reference = args[2];
        var ec = args[3];
        assert new ChannelMapUtil.CB(channel, bank).equals(ChannelMapUtil.e2c3010(ec));

        var cr = ChannelMapUtil.e2cr(type, ec);
        return addElectrode(new Electrode(0, cr.c(), cr.r()));
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T3010base.cpp#L20)
    @Override
    public void stringElectrode(PrintStream out, int channel, Electrode e) {
        var electrode = ChannelMapUtil.cr2e(type, e);
        var cb = ChannelMapUtil.e2c3010(electrode);
        assert cb.channel() == channel;
        out.printf("(%d %d %d %d)", channel, cb.bank(), reference, electrode);
    }
}
