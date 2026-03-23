package io.ast.jneurocarto.probe_npx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NpxProbeTypeTest {

    private static void assertProbeTypeValue(NpxProbeType type, int code, int nShank, int nColShank, int nElectrodeShank, int nChannel, float colSpace, float rowSpace, int shankSpace, int nRef) {
        assertEquals(type.code(), code, "NpxProbeType[" + type.name() + "].code() != " + code);
        assertEquals(type.nShank(), nShank, "NpxProbeType[" + type.name() + "].nShank() != " + nShank);
        assertEquals(type.nColumnPerShank(), nColShank, "NpxProbeType[" + type.name() + "].nColumnPerShank() != " + nColShank);
        assertEquals(type.nElectrodePerShank(), nElectrodeShank, "NpxProbeType[" + type.name() + "].nElectrodePerShank() != " + nElectrodeShank);
        assertEquals(type.nChannel(), nChannel, "NpxProbeType[" + type.name() + "].nChannel() != " + nChannel);
        assertEquals(type.spacePerColumn(), colSpace, "NpxProbeType[" + type.name() + "].spacePerColumn() != " + colSpace);
        assertEquals(type.spacePerRow(), rowSpace, "NpxProbeType[" + type.name() + "].spacePerRow() != " + rowSpace);
        assertEquals(type.spacePerShank(), shankSpace, "NpxProbeType[" + type.name() + "].spacePerShank() != " + shankSpace);
        assertEquals(type.nReference(), nRef, "NpxProbeType[" + type.name() + "].nReference() != " + nRef);
    }

    @Test
    public void np1() {
        assertProbeTypeValue(NpxProbeType.NP0, 0, 1, 2, 960, 384, 32, 20, 0, 5);
    }

    @Test
    public void np21() {
        assertProbeTypeValue(NpxProbeType.NP21, 21, 1, 2, 1280, 384, 32, 15, 0, 6);
    }

    @Test
    public void np24() {
        assertProbeTypeValue(NpxProbeType.NP24, 24, 4, 2, 1280, 384, 32, 15, 250, 21);
    }

    @Test
    public void np1020() {
        assertProbeTypeValue(NpxProbeType.NP1020, 1020, 1, 2, 2496, 384, 87, 20, 0, 9);
        assertProbeTypeValue(NpxProbeType.NP1022, 1020, 1, 2, 2496, 384, 103, 20, 0, 9);
    }

    @Test
    public void np1030() {
        assertProbeTypeValue(NpxProbeType.NP1030, 1030, 1, 2, 4416, 384, 87, 20, 0, 14);
        assertProbeTypeValue(NpxProbeType.NP1032, 1030, 1, 2, 4416, 384, 103, 20, 0, 14);
    }

    @Test
    public void np1100() {
        assertProbeTypeValue(NpxProbeType.NP1100, 1100, 1, 8, 384, 384, 6, 6, 0, 2);
    }

    @Test
    public void np1110() {
        assertProbeTypeValue(NpxProbeType.NP1110, 1110, 1, 8, 6144, 384, 6, 6, 0, 2);
    }

    @Test
    public void np1120() {
        assertProbeTypeValue(NpxProbeType.NP1120, 1120, 1, 2, 384, 384, 4.5F, 4.5F, 0, 2);
    }

    @Test
    public void np1121() {
        assertProbeTypeValue(NpxProbeType.NP1121, 1121, 1, 1, 384, 384, 3, 3, 0, 2);
    }

    @Test
    public void np1122() {
        assertProbeTypeValue(NpxProbeType.NP1122, 1122, 1, 16, 384, 384, 3, 3, 0, 2);
    }

    @Test
    public void np1123() {
        assertProbeTypeValue(NpxProbeType.NP1123, 1123, 1, 12, 384, 384, 4.5F, 4.5F, 0, 2);
    }

    @Test
    public void np1200() {
        assertProbeTypeValue(NpxProbeType.NP1200, 1200, 1, 2, 128, 128, 32, 31, 0, 2);
    }

    @Test
    public void np1300() {
        assertProbeTypeValue(NpxProbeType.NP1300, 1300, 1, 2, 960, 384, 48, 20, 0, 5);
    }

    @Test
    public void np2003() {
        assertProbeTypeValue(NpxProbeType.NP2003, 2003, 1, 2, 1280, 384, 32, 15, 0, 3);
    }

    @Test
    public void np2013() {
        assertProbeTypeValue(NpxProbeType.NP2013, 2013, 4, 2, 1280, 384, 32, 15, 250, 6);
    }

    @Test
    public void np2020() {
        assertProbeTypeValue(NpxProbeType.NP2020, 2020, 4, 2, 1280, 1536, 32, 15, 250, 3);
    }

    @Test
    public void np3000() {
        assertProbeTypeValue(NpxProbeType.NP3000, 3000, 1, 2, 128, 128, 15, 15, 0, 2);
    }

    @Test
    public void np3010() {
        assertProbeTypeValue(NpxProbeType.NP3010, 3010, 1, 2, 1280, 912, 32, 15, 0, 3);
    }

    @Test
    public void np3020() {
        assertProbeTypeValue(NpxProbeType.NP3020, 3020, 4, 2, 1280, 912, 32, 15, 250, 7);
    }

}
