package io.ast.jneurocarto.javafx.view;

import javafx.scene.Node;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.app.Application;
import io.ast.jneurocarto.javafx.app.PluginSetupService;

/// The plugin of jNeuroCarto javafx-application.
///
/// ### dependencies injection
///
/// The parameters of the constructor of the plugin are consider the dependencies of the plugin.
/// The supported types are:
/// * [PluginSetupService]
/// * [Application]
/// * [io.ast.jneurocarto.core.cli.CartoConfig]
/// * [io.ast.jneurocarto.core.config.Repository]
/// * [io.ast.jneurocarto.core.ProbeDescription] or its subclasses
/// * subclasses of [Plugin].
///
/// ### life cycles
///
/// 1. Declared by [Provide] onto a [PluginProvider].
/// 2. [PluginProvider] loaded via ServiceLoader.
/// 3. [Plugin] classes loaded via reflection over [Provide].
/// 4. [PluginProvider] blocks [Plugin] ([PluginProvider#filterPlugin(PluginSetupService, Class)])
/// 5. If the [Plugin] is unnamed (empty [Provide#name()]), initialize it.
/// 6. Check loading list ([io.ast.jneurocarto.core.config.CartoUserConfig#views] and [io.ast.jneurocarto.core.cli.CartoConfig#extraViewList])
/// 7. initialize [Plugin]s in the list via their constructor (dependencies injected).
/// 8. setup initialized [Plugin]s via ([Plugin#setup(PluginSetupService)]).
public interface Plugin {

    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Setup the UIs and other staffs.
     *
     * @param service
     * @return javafx graphics node, which will be placed at the right panel of the application.
     */
    @Nullable
    Node setup(PluginSetupService service);
}
