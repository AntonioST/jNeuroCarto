package io.ast.jneurocarto.javafx.view;

import javafx.scene.Node;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.app.PluginSetupService;

public interface Plugin {

    default String name() {
        return getClass().getSimpleName();
    }

    @Nullable
    Node setup(PluginSetupService service);
}
