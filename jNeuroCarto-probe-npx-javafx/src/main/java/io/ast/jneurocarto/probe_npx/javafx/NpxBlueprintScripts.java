package io.ast.jneurocarto.probe_npx.javafx;

import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.script.BlueprintScript;
import io.ast.jneurocarto.javafx.script.ScriptParameter;
import io.ast.jneurocarto.probe_npx.ChannelMap;
import io.ast.jneurocarto.probe_npx.ChannelMaps;

@BlueprintScript()
public final class NpxBlueprintScripts {
    private NpxBlueprintScripts() {
        throw new RuntimeException();
    }

    @BlueprintScript("npx24_single_shank")
    public static void newNpx24Singleshank(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "shank", defaultValue = "0") int shank,
      @ScriptParameter(value = "row", defaultValue = "0") double row) {
        bp.setChannelmap(ChannelMaps.npx24SingleShank(shank, row));
    }

    @BlueprintScript("npx24_stripe")
    public static void npx24Stripe(
      BlueprintAppToolkit<ChannelMap> bp,
      @ScriptParameter(value = "row", defaultValue = "0") double row) {
        bp.setChannelmap(ChannelMaps.npx24Stripe(row));
    }


}
