package io.ast.jneurocarto.javafx.app;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.javafx.view.GlobalStateView;
import io.ast.jneurocarto.javafx.view.StateView;

public final class PluginStateService {

    private static final Logger log = LoggerFactory.getLogger(PluginStateService.class);

    private PluginStateService() {
        throw new RuntimeException();
    }

    public static <S> @Nullable S loadState(StateView<S> plugin) {
        log.debug("loadState({})", plugin.getClass().getSimpleName());

        var application = Application.getInstance();
        var repository = application.getRepository();

        if (plugin instanceof GlobalStateView<?> global) {
            return repository.getGlobalConfig((Class<S>) global.getStateClass());
        } else {
            return repository.getViewConfig(plugin.getStateClass());
        }
    }

    public static <S> void saveState(StateView<S> plugin, S state) {
        log.debug("saveState({})", plugin.getClass().getSimpleName());

        var application = Application.getInstance();
        var repository = application.getRepository();

        if (plugin instanceof GlobalStateView) {
            repository.setGlobalConfig(state);
        } else {
            repository.setViewConfig(state);
        }
    }

    public static void retrieveAllStates() {
        log.debug("retrieveAllStates");

        var application = Application.getInstance();

        for (var plugin : application.plugins) {
            if (plugin instanceof StateView<?> view) {
                view.restoreState();
            }
        }
    }

    public static void saveAllStates() {
        log.debug("saveAllStates");

        var application = Application.getInstance();

        for (var plugin : application.plugins) {
            if (plugin instanceof StateView<?> view) {
                view.saveState();
            }
        }
    }

}
