@file:BlueprintScript()

package io.ast.jneurocarto.script.probe_npx

import java.nio.file.Path
import io.ast.jneurocarto.core.RequestChannelmap
import io.ast.jneurocarto.core.blueprint.BlueprintToolkit
import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit
import io.ast.jneurocarto.javafx.script.BlueprintScript
import io.ast.jneurocarto.javafx.script.ScriptParameter
import io.ast.jneurocarto.probe_npx.ChannelMap
import io.ast.jneurocarto.probe_npx.NpxProbeDescription
import io.ast.jneurocarto.probe_npx.javafx.DataVisualizePlugin
import io.ast.jneurocarto.script.*

@BlueprintScript(
    value = "blueprint_simple_init_script_from_activity_data_with_a_threshold",
    description = """
Initial a blueprint based on the experimental activity data with a given threshold,
which follows:

* set NaN area as excluded zone.
* set full-density zone where corresponding activity over the threshold.
* make the full-density zone into rectangle by filling the gaps.
* extend the full-density zone with half-density zone.
"""
)
@RequestChannelmap(probe = NpxProbeDescription::class, code = "NP24")
fun blueprintSimpleInitScriptFromActivityDataWithThreshold(
    toolkit: BlueprintAppToolkit<ChannelMap>,
    @ScriptParameter(value = "filename", description = "a numpy filepath, which shape Array[int, N, (shank, col, row, state, value)]")
    filename: Path,
    @ScriptParameter(value = "threshold", description = "activities threshold to set FULL category")
    threshold: Double
) {
    toolkit.printLogMessage("filename=$filename, threshold=$threshold")

    var data = toolkit.loadNumpyBlueprintData(filename)
    data[data eq 0.0] = Double.NaN
    data = toolkit.interpolateNaN(data, 3, BlueprintToolkit.InterpolateMethod.mean)

    val F = NpxProbeDescription.CATE_FULL
    val H = NpxProbeDescription.CATE_HALF
    val Q = NpxProbeDescription.CATE_QUARTER
    val L = NpxProbeDescription.CATE_LOW
    val X = NpxProbeDescription.CATE_EXCLUDED

    toolkit.printLogMessage("min=${data.nanmin().round(2)}, max=${data.nanmax().round(2)}")

    toolkit.getPlugin(DataVisualizePlugin::class.java).ifPresent { plugin ->
        plugin.file = filename
        plugin.interpolate = 3
        plugin.updateDataImage(data)
        plugin.repaint()
    }

    toolkit.clear()
    toolkit[isnan(data)] = X
    toolkit.reduce(X, 20, threshold = 0..20)
    toolkit.fill(X, 10..Int.MAX_VALUE)

    toolkit[data ge threshold] = F
    toolkit.fill(F)
    toolkit.extend(F, 2, threshold = 0..100)
    toolkit.extend(F, 10, H)

    toolkit.applyViewBlueprint()
}