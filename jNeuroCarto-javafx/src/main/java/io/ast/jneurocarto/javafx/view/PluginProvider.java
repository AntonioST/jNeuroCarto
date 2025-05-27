package io.ast.jneurocarto.javafx.view;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.javafx.app.PluginSetupService;

@NullMarked
public interface PluginProvider {

    String description();

    default boolean filterPlugin(PluginSetupService service, Class<? extends Plugin> plugin) {
        return true;
    }
}
