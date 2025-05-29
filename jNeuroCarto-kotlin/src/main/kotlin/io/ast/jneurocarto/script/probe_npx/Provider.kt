package io.ast.jneurocarto.script.probe_npx

import io.ast.jneurocarto.javafx.script.CheckProbe
import io.ast.jneurocarto.javafx.view.PluginProvider
import io.ast.jneurocarto.probe_npx.NpxProbeDescription

@CheckProbe(probe = NpxProbeDescription::class)
class Provider : PluginProvider {
    override fun description(): String {
        return "Example"
    }
}