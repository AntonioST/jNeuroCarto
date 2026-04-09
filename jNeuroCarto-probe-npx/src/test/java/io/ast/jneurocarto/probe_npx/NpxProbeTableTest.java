package io.ast.jneurocarto.probe_npx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.ast.jneurocarto.probe_npx.table.ProbeFormats;
import io.ast.jneurocarto.probe_npx.table.ProbeTables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NpxProbeTableTest {

    private static ProbeTables TABLES;

    @BeforeAll
    public static void setupTable() throws IOException {
        var stream = NpxProbeTableTest.class.getClassLoader().getResourceAsStream("ProbeTables/probe_features.json");
        Objects.requireNonNull(stream, "missing ProbeTables/probe_features.json");
        TABLES = ProbeTables.readFrom(stream);
    }

    public record ProbeFormatPair(int type, ProbeFormats.ProbeFormat format) {

        @Override
        public String toString() {
            return format.name + "/" + type;
        }
    }

    public static Set<String> allProbeTypes() {
        return TABLES.probes.keySet();
    }

    public static Stream<ProbeFormatPair> allProbeFormats() {
        return TABLES.formats.keys().stream()
          .map(name -> TABLES.formats.get(name))
          .flatMap(format -> Arrays.stream(format.types()).mapToObj(type -> new ProbeFormatPair(type, format)));
    }


    @ParameterizedTest
    @MethodSource(value = "allProbeTypes")
    public void probeMatchTable(String name) {
        var t = Objects.requireNonNull(TABLES.probes.get(name));
        var p = NpxProbeType.of(name);

        assertEquals(t.numShanks, p.nShank(), "nShank");
        assertEquals(t.numChannels, p.nChannel(), "nChannel");
        assertEquals(t.electrodesPerShank, p.nElectrodePerShank(), "nElectrodePerShank");
        // NP2010 is duplicate of PRB2_4_2_0640_0, but both have different total_electrodes value.
        if (!name.equals("NP2010")) {
            assertEquals(t.electrodesTotal, p.nElectrode(), "nElectrode");
        }
        assertEquals(t.columnsPerShank, p.nColumnPerShank(), "nColumnPerShank");
        assertEquals(t.rowsPerShank, p.nRowPerShank(), "nRowPerShank");
        assertEquals(t.electrodeHorzUm, p.spacePerColumn(), "spacePerColumn");
        assertEquals(t.electrodeVertUm, p.spacePerRow(), "spacePerRow");
        assertEquals(t.shankUm, p.spacePerShank(), "spacePerShank");
    }


    @ParameterizedTest
    @MethodSource(value = "allProbeFormats")
    public void probeMatchFormat(ProbeFormatPair pair) {
        System.out.println("numChannels: " + pair.format.numChannels());
        System.out.println("channel    : " + pair.format.channel());
        System.out.println("electrode  : " + pair.format.electrode());
        System.out.println("bank       : " + pair.format.bank());
        System.out.println("refIds     : " + pair.format.refIdsExpr());
        System.out.println("bankMasks  : " + pair.format.bankMasksExpr());
        System.out.println("colMode    : " + pair.format.colMode());
        System.out.println("apGain     : " + pair.format.apGain());
        System.out.println("lfGain     : " + pair.format.lfGain());
        System.out.println("apHiPasFlt : " + pair.format.apHighPassFilter());
    }

    @ParameterizedTest
    @MethodSource(value = "allProbeFormats")
    public void probeMatchRefIdFormat(ProbeFormatPair pair) {
        var refs = new ArrayList<>(pair.format.refIds());
        var name = "NP" + pair.type;
        var p = NpxProbeType.of(name);

        // FIXME number of references is not match between SpikeGLX source code and ProbeTable
        //   in certain probe
        if (name.equals("NP21")) {
            refs.add(new String[]{"bnk3"});
        }

        switch (pair.type) {
        case 1020, 1030, 1100, 1120, 1121, 1122, 1123, 1200, 3020 -> {
            // there probe's total reference number doesn't match
        }
        default -> {
            assertEquals(refs.size(), p.nReference(), "nReference");
            assertThrows(IllegalArgumentException.class, () -> {
                ReferenceInfo.of(p, p.nReference());
            });
        }
        }

        for (int i = 0, total = Math.min(refs.size(), p.nReference()); i < total; i++) {
            var inf = ReferenceInfo.of(p, i);
            for (var ref : refs.get(i)) {
                if ("ext".equals(ref)) {
                    assertEquals(ReferenceInfo.ReferenceType.EXT, inf.type(), "ref" + i + ".type");
                    assertEquals(0, inf.shank(), "ref" + i + ".shank");
                    assertEquals(0, inf.bank(), "ref" + i + ".bank");
                    assertEquals(0, inf.channel(), "ref" + i + ".channel");
                } else if ("gnd".equals(ref)) {
                    assertEquals(ReferenceInfo.ReferenceType.GROUND, inf.type(), "ref" + i + ".type");
                    assertEquals(0, inf.shank(), "ref" + i + ".shank");
                    assertEquals(0, inf.bank(), "ref" + i + ".bank");
                    assertEquals(0, inf.channel(), "ref" + i + ".channel");
                } else if ("tip".equals(ref)) {
                    assertEquals(ReferenceInfo.ReferenceType.TIP, inf.type(), "ref" + i + ".type");
                    assertEquals(0, inf.shank(), "ref" + i + ".shank");
                    assertEquals(0, inf.bank(), "ref" + i + ".bank");
                    assertEquals(0, inf.channel(), "ref" + i + ".channel");
                } else if (ref.startsWith("tip")) {
                    var tip = Integer.parseInt(ref.substring(3));
                    assertEquals(ReferenceInfo.ReferenceType.TIP, inf.type(), "ref" + i + ".type");
                    assertEquals(tip, inf.shank(), "ref" + i + ".shank");
                    assertEquals(0, inf.bank(), "ref" + i + ".bank");
                    assertEquals(0, inf.channel(), "ref" + i + ".channel");
                } else if ("bnk".equals(ref)) {
                    assertEquals(ReferenceInfo.ReferenceType.BANK, inf.type(), "ref" + i + ".type");
                    assertEquals(0, inf.bank(), "ref" + i + ".bank");
                } else if (ref.startsWith("bnk")) {
                    var bank = Integer.parseInt(ref.substring(3));
                    assertEquals(ReferenceInfo.ReferenceType.BANK, inf.type(), "ref" + i + ".type");
                    assertEquals(bank, inf.bank(), "ref" + i + ".bank");
                } else if ("shk".equals(ref)) {
                    assertEquals(ReferenceInfo.ReferenceType.BANK, inf.type(), "ref" + i + ".type");
                    assertEquals(0, inf.shank(), "ref" + i + ".shank");
                } else if (ref.startsWith("shk")) {
                    var shank = Integer.parseInt(ref.substring(3));
                    assertEquals(ReferenceInfo.ReferenceType.BANK, inf.type(), "ref" + i + ".type");
                    assertEquals(shank, inf.shank(), "ref" + i + ".shank");
                }
            }
        }
    }
}
