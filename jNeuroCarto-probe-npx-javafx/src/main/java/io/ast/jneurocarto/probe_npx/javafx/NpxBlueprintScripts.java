package io.ast.jneurocarto.probe_npx.javafx;

import java.util.Arrays;
import java.util.function.Function;

import io.ast.jneurocarto.core.blueprint.BlueprintMask;
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.script.BlueprintScript;
import io.ast.jneurocarto.javafx.script.CheckProbe;
import io.ast.jneurocarto.javafx.script.PyValue;
import io.ast.jneurocarto.javafx.script.ScriptParameter;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;
import io.ast.jneurocarto.probe_npx.NpxProbeDescription;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

@BlueprintScript()
@CheckProbe(probe = NpxProbeDescription.class)
public final class NpxBlueprintScripts {

    @BlueprintScript(value = "npx24_single_shank", description = """
      Make a block channelmap for 4-shank Neuropixels probe.
      """)
    @CheckProbe(code = "NP24")
    public void newNpx24Singleshank(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "shank", defaultValue = "0",
        description = "on which shank") int shank,
      @ScriptParameter(value = "row", defaultValue = "0",
        description = "start row in um.") double row) {
        bp.setChannelmap(ChannelMaps.npx24SingleShank(shank, row));
    }

    @BlueprintScript(value = "npx24_stripe", description = """
      Make a block channelmap for 4-shank Neuropixels probe.
      """)
    @CheckProbe(code = "NP24")
    public void npx24Stripe(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "row", defaultValue = "0",
        description = "start row in um.") double row) {
        bp.setChannelmap(ChannelMaps.npx24Stripe(row));
    }

    public sealed interface ShankOrSelect {
        record OneShank(int shank) implements ShankOrSelect {
            public OneShank {
                if (shank < 0 || shank >= 4) {
                    throw new IllegalArgumentException();
                }
            }
        }

        record TwoShank(int s1, int s2) implements ShankOrSelect {
            public TwoShank {
                if (s1 < 0 || s1 >= 4) {
                    throw new IllegalArgumentException();
                }
                if (s2 < 0 || s2 >= 4) {
                    throw new IllegalArgumentException();
                }
                if (s1 == s2) {
                    throw new IllegalArgumentException();
                }
            }
        }

        record AllShank() implements ShankOrSelect {
        }

        record Select() implements ShankOrSelect, Function<PyValue, ShankOrSelect> {
            @Override
            public ShankOrSelect apply(PyValue s) {
                if (s instanceof PyValue.PyList list) {
                    s = list.asTuple();
                } else if (s instanceof PyValue.PySymbol symbol) {
                    s = symbol.asStr();
                }

                switch (s) {
                case PyValue.PyNone _:
                    return new AllShank();
                case PyValue.PyStr(String content):
                    switch (content) {
                    case "selected":
                        return this;
                    case "all":
                        return new AllShank();
                    }
                    throw new RuntimeException();
                case PyValue.PyTuple1(PyValue.PyInt(int shank)):
                    return new OneShank(shank);
                case PyValue.PyTuple2(PyValue.PyInt(int s1), PyValue.PyInt(int s2)):
                    return new TwoShank(s1, s2);
                default:
                    throw new RuntimeException();
                }
            }
        }
    }


    @BlueprintScript(value = "npx24_half_density", description = """
      Make a channelmap for 4-shank Neuropixels probe that uniformly distributes channels in *half* density.
      """)
    @CheckProbe(code = "NP24")
    public void npx24HalfDensity(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "shank", type = "int|[int,int]|'selected'", defaultValue = "selected",
        converter = ShankOrSelect.Select.class,
        description = "on which shank/s. Use 'select' for selected electrodes.") ShankOrSelect shank,
      @ScriptParameter(value = "row", defaultValue = "0",
        description = "start row in um.") double row) {

        switch (shank) {
        case ShankOrSelect.Select _ -> npx24HalfDensity(bp, row);
        case ShankOrSelect.OneShank(var s) -> bp.setChannelmap(ChannelMaps.npx24HalfDensity(s, row));
        case ShankOrSelect.TwoShank(var s1, var s2) -> bp.setChannelmap(ChannelMaps.npx24HalfDensity(s1, s2, row));
        case ShankOrSelect.AllShank _ -> throw new RuntimeException("need at most two shanks");
        }
    }

    private void npx24HalfDensity(BlueprintAppToolkit<ChannelMap> bp, double row) {
        var index = bp.getAllCaptureElectrodes();
        var z = index.length;
        if (z < 4) {
            bp.printLogMessage("need more electrodes");
            return;
        }

        var r = (int) (row / NpxProbeType.NP24.spacePerRow());
        if (r % 2 == 0) {
            z = keepIndexForModule(index, r, 0, 3);
        } else {
            z = keepIndexForModule(index, r, 1, 2);
        }

        Arrays.sort(index);
        bp.addElectrode(index, 0, z);
        bp.clearCaptureElectrodes();
    }

    private static int keepIndexForModule(int[] index, int mod, int m1, int m2) {
        var ret = index.length;
        for (int i = 0, length = ret; i < length; i++) {
            var m = index[i] % mod;
            if (!(m == m1 || m == m2)) {
                index[i] = Integer.MAX_VALUE;
                ret--;
            }
        }
        return ret;
    }

    @BlueprintScript(value = "npx24_quarter_density", description = """
      Make a channelmap for 4-shank Neuropixels probe that uniformly distributes channels in *quarter* density.
      """)
    @CheckProbe(code = "NP24")
    public void npx24QuarterDensity(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "shank", type = "int|[int,int]|'selected'|'None'", defaultValue = "None",
        converter = ShankOrSelect.Select.class,
        description = "on which shank/s. Use 'select' for selected electrodes. Use ``all`` for four shanks.") ShankOrSelect shank,
      @ScriptParameter(value = "row", defaultValue = "0",
        description = "start row in um.") double row) {

        switch (shank) {
        case ShankOrSelect.Select _ -> npx24QuarterDensity(bp, row);
        case ShankOrSelect.OneShank(var s) -> bp.setChannelmap(ChannelMaps.npx24QuarterDensity(s, row));
        case ShankOrSelect.TwoShank(var s1, var s2) -> bp.setChannelmap(ChannelMaps.npx24QuarterDensity(s1, s2, row));
        case ShankOrSelect.AllShank _ -> bp.setChannelmap(ChannelMaps.npx24QuarterDensity(row));
        }
    }

    private void npx24QuarterDensity(BlueprintAppToolkit<ChannelMap> bp, double row) {
        var index = bp.getAllCaptureElectrodes();
        var z = index.length;
        if (z < 8) {
            bp.printLogMessage("need more electrodes");
            return;
        }

        var r = (int) (row / NpxProbeType.NP24.spacePerRow());
        z = switch (r % 4) {
            case 0 -> keepIndexForModule(index, 8, 0, 5);
            case 1 -> keepIndexForModule(index, 8, 1, 4);
            case 2 -> keepIndexForModule(index, 8, 2, 7);
            case 3 -> keepIndexForModule(index, 8, 3, 6);
            default -> throw new RuntimeException("unreachable");
        };

        Arrays.sort(index);
        bp.addElectrode(index, 0, z);
        bp.clearCaptureElectrodes();
    }

    @BlueprintScript(value = "npx24_one_eighth_density", description = """
      Make a channelmap for 4-shank Neuropixels probe that uniformly distributes channels in *one-eighth* density.
      """)
    @CheckProbe(code = "NP24")
    public void npx24OneEightDensity(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "row", defaultValue = "0",
        description = "start row in um.") double row) {
        bp.setChannelmap(ChannelMaps.npx24OneEightDensity(row));
    }

    private BlueprintMask maskForShank(BlueprintAppToolkit<ChannelMap> bp, PyValue shank) {
        if (shank instanceof PyValue.PyNone) {
            return bp.mask(true);
        } else if (shank instanceof PyValue.PyList list) {
            var s = list.toIntArray();
            Arrays.sort(s);
            return bp.mask(e -> Arrays.binarySearch(s, e.s()) >= 0);
        } else {
            throw new ClassCastException("cannot cast " + shank + " into int[]");
        }
    }

    @BlueprintScript(value = "move_blueprint", description = """
      Move blueprint upward or downward.
      """)
    @CheckProbe(code = "NP24", create = false)
    public void moveShanks(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "y",
        description = "um") double y,
      @ScriptParameter(value = "shank", type = "list[int]", defaultValue = "None",
        description = "only particular shanks.") PyValue shank,
      @ScriptParameter(value = "update", defaultValue = "False",
        description = "update channelmap to follow the blueprint change.") boolean update) {

        var r = (int) (y / NpxProbeType.NP24.spacePerRow());
        var x = maskForShank(bp, shank);
        bp.move(r, x);
        bp.applyViewBlueprint();

        if (update) {
            bp.refreshElectrodeSelection();
        }
    }

    @BlueprintScript(value = "exchange_shank", description = """
      Move blueprint between shanks.
      """)
    @CheckProbe(code = "NP24", create = false)
    public void exchangeShanks(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "shank",
        description = """
          For N shank probe, it is an N-length list.
          For example, ``[3, 2, 1, 0]`` gives a reverse-shank-ordered blueprint.
          """) int[] shank,
      @ScriptParameter(value = "update", defaultValue = "False",
        description = "update channelmap to follow the blueprint change.") boolean update) {
        if (shank.length != 4) {
            throw new RuntimeException("not a 4-length list");
        }

        var mask = new BlueprintMask[]{
          bp.mask(e -> e.s() == 0),
          bp.mask(e -> e.s() == 1),
          bp.mask(e -> e.s() == 2),
          bp.mask(e -> e.s() == 3),
        };

        var bq = bp.clone();
        bq.clear();

        for (int i = 0; i < 4; i++) {
            bq.from(mask[i], bp, mask[shank[i]]);
        }

        bq.applyViewBlueprint();

        if (update) {
            bq.refreshElectrodeSelection();
        }
    }
}
