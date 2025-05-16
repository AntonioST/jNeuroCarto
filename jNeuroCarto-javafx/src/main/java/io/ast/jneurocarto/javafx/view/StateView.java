package io.ast.jneurocarto.javafx.view;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.app.PluginStateService;

@NullMarked
public interface StateView<S> {
    Class<S> getStateClass();

    @Nullable
    S getState();

    void restoreState(@Nullable S state);

    default void saveState() {
        var state = getState();
        if (state != null) saveState(state);
    }

    default void saveState(S state) {
        PluginStateService.saveState(this, state);
    }

    default void restoreState() {
        restoreState(PluginStateService.loadState(this));
    }

}
