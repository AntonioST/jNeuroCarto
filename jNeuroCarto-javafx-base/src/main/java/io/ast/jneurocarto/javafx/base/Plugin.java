package io.ast.jneurocarto.javafx.base;

import io.ast.jneurocarto.config.cli.CartoConfig;
import javafx.scene.Node;

public interface Plugin {

    default String name() {
        return getClass().getSimpleName();
    }

    String description();

    Node setup(CartoConfig config);
}
