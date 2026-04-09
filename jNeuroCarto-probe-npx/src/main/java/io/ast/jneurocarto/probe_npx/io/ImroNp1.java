package io.ast.jneurocarto.probe_npx.io;

import java.io.PrintStream;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.Electrode;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@NullMarked
class ImroNp1 extends ImroIO implements ImroIO.RestrictedGainValue {

    protected ImroNp1(NpxProbeType type) {
        super(type);
    }

    static final int[] AP_GAIN_LIST = {50, 125, 250, 500, 1000, 1500, 2000, 3000};
    static final int[] LF_GAIN_LIST = {50, 125, 250, 500, 1000, 1500, 2000, 3000};

    @Override
    public int[] apGain() {
        return AP_GAIN_LIST;
    }

    @Override
    public int[] lfGain() {
        return LF_GAIN_LIST;
    }

    @Override
    public boolean parseElectrodes(int[] args) {
        assert args.length == 6;
        var ch = args[0];
        var bk = args[1];
        reference = args[2];
        var ap = args[3];
        var lf = args[4];
        var ft = args[5];
        var ed = ChannelMapUtil.c2e0(ch, bk);
        var cr = ChannelMapUtil.e2cr(type, ed);
        assert cr.s() == 0;

        var e = new Electrode(0, cr.c(), cr.r());
        e.apBandGain = checkApGainValue(ap, 250);
        e.lfBandBain = checkLfGainValue(lf, 250);
        e.apHpFilter = ft != 0;

        return addElectrode(e);
    }

    @Override
    public void stringElectrode(PrintStream out, int channel, Electrode e) {
        var cb = ChannelMapUtil.e2cb(type, e);
        assert cb.channel() == channel;
        var ap = checkApGainValue(e.apBandGain);
        var lf = checkLfGainValue(e.lfBandBain);
        out.printf("(%d %d %d %d %d %d)", channel, cb.bank(), reference, ap, lf, e.apHpFilter ? 1 : 0);
    }
}
