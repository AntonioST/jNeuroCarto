package io.ast.jneurocarto.javafx.view;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.javafx.app.PluginSetupService;

/// [Plugin] provider.
///
/// Possible annotations:
/// * [Provide] provided plugins.
/// * [io.ast.jneurocarto.core.RequestChannelmap] indicated this plugin group only service for certain probe family.
@NullMarked
public interface PluginProvider {

    /**
     * @return plugin description
     */
    String description();

    /**
     * specific filtering provided plugins depends on runtime condition.
     *
     * @param service
     * @param plugin  class of plugins
     * @return use {@code false} to block certain plugin.
     */
    default boolean filterPlugin(PluginSetupService service, Class<? extends Plugin> plugin) {
        return true;
    }
}
