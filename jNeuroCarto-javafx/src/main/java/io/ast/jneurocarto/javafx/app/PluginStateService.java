package io.ast.jneurocarto.javafx.app;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.javafx.view.GlobalStateView;
import io.ast.jneurocarto.javafx.view.StateView;

public final class PluginStateService {

    private PluginStateService() {
        throw new RuntimeException();
    }

    public static <S> @Nullable S loadState(StateView<S> plugin) {
        var application = Application.getInstance();
        var repository = application.getRepository();

        if (plugin instanceof GlobalStateView global) {
            return (S) repository.getGlobalConfig(global.getStateClass());
        } else {
            return repository.getViewConfig(plugin.getStateClass());
        }
    }

    public static <S> void saveState(StateView<S> plugin, S state) {
        var application = Application.getInstance();
        var repository = application.getRepository();

        if (plugin instanceof GlobalStateView) {
            repository.setGlobalConfig(state);
        } else {
            repository.setViewConfig(state);
        }
    }

    public static void retrieveAllStates() {
        var application = Application.getInstance();

        for (var plugin : application.plugins) {
            if (plugin instanceof StateView<?> view) {
                view.restoreState();
            }
        }
    }

    public static void saveAllStates() {
        var application = Application.getInstance();

        for (var plugin : application.plugins) {
            if (plugin instanceof StateView<?> view) {
                view.saveState();
            }
        }
    }

}
