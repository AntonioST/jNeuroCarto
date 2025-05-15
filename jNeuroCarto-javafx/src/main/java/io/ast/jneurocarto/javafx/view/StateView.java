package io.ast.jneurocarto.javafx.view;

public interface StateView<S> {

    S saveState();

    void restoreState(S state);

    static void saveState(StateView<?> plugin) {
    }

    static void restoreState(StateView<?> plugin) {
    }
}
