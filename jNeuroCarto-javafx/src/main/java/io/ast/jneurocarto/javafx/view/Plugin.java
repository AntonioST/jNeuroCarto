package io.ast.jneurocarto.javafx.view;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.app.PluginSetupService;
import javafx.scene.Node;

public interface Plugin {

    default String name() {
        return getClass().getSimpleName();
    }

    String description();

    @Nullable
    Node setup(PluginSetupService service);
}
