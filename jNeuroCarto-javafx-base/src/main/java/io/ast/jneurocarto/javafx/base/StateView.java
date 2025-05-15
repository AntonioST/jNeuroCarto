package io.ast.jneurocarto.javafx.base;

public interface StateView<S> {

    S saveState();

    void restoreState(S state);

    static void saveState(StateView<?> plugin) {
    }

    static void restoreState(StateView<?> plugin) {
    }
}
