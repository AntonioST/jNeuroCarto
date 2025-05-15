package io.ast.jneurocarto.javafx.view;

import javafx.scene.Node;

public interface Plugin {

    default String name() {
        return getClass().getSimpleName();
    }

    String description();

    Node setup();
}
