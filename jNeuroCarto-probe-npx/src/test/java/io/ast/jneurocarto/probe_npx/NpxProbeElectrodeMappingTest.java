package io.ast.jneurocarto.probe_npx;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class NpxProbeElectrodeMappingTest {

    record Fail(int shank, int electrode, int channel, int bank, int value) {
    }

    private static void assertForAllShankAllElectrode(NpxProbeType type, int shank, int electrode) {
        List<Fail> fails = new ArrayList<>();
        for (int s = 0; s < shank; s++) {
            for (int e = 0; e < electrode; e++) {
                var cb = ChannelMapUtil.e2cb(type, s, e);
                var ee = ChannelMapUtil.c2e(type, cb.channel(), cb.bank(), s);
                if (e != ee) {
                    fails.add(new Fail(s, e, cb.channel(), cb.bank(), ee));
                }
            }
        }
        if (!fails.isEmpty()) {
            fail(buildFailMessage(fails));
        }
    }

    private static void assertForAllShankArrElectrode(NpxProbeType type, int shank, int electrode) {
        List<Fail> fails = new ArrayList<>();
        int[] es = new int[electrode];
        for (int i = 0; i < electrode; i++) {
            es[i] = i;
        }

        for (int s = 0; s < shank; s++) {
            var cb = ChannelMapUtil.e2cb(type, s, es);
            assertEquals(2, cb.length);
            assertEquals(es.length, cb[0].length);
            assertEquals(es.length, cb[1].length);

            var ee = ChannelMapUtil.c2e(type, cb, s);
            assertEquals(es.length, ee.length);

            for (int i = 0; i < electrode; i++) {
                if (es[i] != ee[i]) {
                    fails.add(new Fail(s, es[i], cb[0][i], cb[1][i], ee[i]));
                }
            }
        }

        if (!fails.isEmpty()) {
            fail(buildFailMessage(fails));
        }
    }

    private static void assertForArrShankArrElectrode(NpxProbeType type, int shank, int electrode) {
        List<Fail> fails = new ArrayList<>();
        int[] ss = new int[shank * electrode];
        int[] es = new int[shank * electrode];
        for (int i = 0; i < shank * electrode; i++) {
            ss[i] = i / electrode;
            es[i] = i % electrode;
        }

        var cb = ChannelMapUtil.e2cb(type, ss, es);
        assertEquals(2, cb.length);
        assertEquals(es.length, cb[0].length);
        assertEquals(es.length, cb[1].length);

        var ee = ChannelMapUtil.c2e(type, cb, ss);
        assertEquals(es.length, ee.length);

        for (int i = 0; i < shank * electrode; i++) {
            if (es[i] != ee[i]) {
                var s = i / electrode;
                fails.add(new Fail(s, es[i], cb[0][i], cb[1][i], ee[i]));
            }
        }


        if (!fails.isEmpty()) {
            fail(buildFailMessage(fails));
        }
    }

    private static String buildFailMessage(List<Fail> fails) {
        var builder = new StringBuilder();
        for (var fail : fails) {
            builder.append("expect electrode ").append(fail.electrode).append(" on shank ").append(fail.shank)
              .append(", but got electrode ").append(fail.value).append(" back, where channel ").append(fail.channel)
              .append(" and bank ").append(fail.bank).append(".\n");
        }
        return builder.toString();
    }

    @Test
    public void np1() {
        var t = NpxProbeType.np0;
        assertForAllShankAllElectrode(t, t.nShank(), t.nElectrode());
        assertForAllShankArrElectrode(t, t.nShank(), t.nElectrode());
        assertForArrShankArrElectrode(t, t.nShank(), t.nElectrode());
    }

    @Test
    public void np21() {
        var t = NpxProbeType.np21;
        assertForAllShankAllElectrode(t, t.nShank(), t.nElectrode());
        assertForAllShankArrElectrode(t, t.nShank(), t.nElectrode());
        assertForArrShankArrElectrode(t, t.nShank(), t.nElectrode());
    }

    @Test
    public void np24() {
        var t = NpxProbeType.np24;
        assertForAllShankAllElectrode(t, t.nShank(), t.nElectrode());
        assertForAllShankArrElectrode(t, t.nShank(), t.nElectrode());
        assertForArrShankArrElectrode(t, t.nShank(), t.nElectrode());
    }

    @Test
    public void np1110() {
        var t = NpxProbeType.np1110;
        assertForAllShankAllElectrode(t, t.nShank(), t.nElectrode());
        assertForAllShankArrElectrode(t, t.nShank(), t.nElectrode());
        assertForArrShankArrElectrode(t, t.nShank(), t.nElectrode());
    }

    @Test
    public void np2020() {
        var t = NpxProbeType.np2020;
        assertForAllShankAllElectrode(t, t.nShank(), t.nElectrode());
        assertForAllShankArrElectrode(t, t.nShank(), t.nElectrode());
        assertForArrShankArrElectrode(t, t.nShank(), t.nElectrode());
    }

    @Test
    public void np3010() {
        var t = NpxProbeType.np3010;
        assertForAllShankAllElectrode(t, t.nShank(), t.nElectrode());
        assertForAllShankArrElectrode(t, t.nShank(), t.nElectrode());
        assertForArrShankArrElectrode(t, t.nShank(), t.nElectrode());
    }

    @Test
    public void np3020() {
        var t = NpxProbeType.np3020;
        assertForAllShankAllElectrode(t, t.nShank(), t.nElectrode());
        assertForAllShankArrElectrode(t, t.nShank(), t.nElectrode());
        assertForArrShankArrElectrode(t, t.nShank(), t.nElectrode());
    }
}
