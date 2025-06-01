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

    public static <S> @Nullable S loadLocalState(Class<S> state) {
        log.debug("loadLocalState({})", state.getSimpleName());
        return Application.getInstance().getRepository().getViewConfig(state);
    }

    public static <S> @Nullable S loadGlobalState(Class<S> state) {
        log.debug("loadGlobalState({})", state.getSimpleName());
        return Application.getInstance().getRepository().getGlobalConfig(state);
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

    public static <S> void saveLocalState(S state) {
        log.debug("saveLocalState({})", state.getClass().getSimpleName());
        Application.getInstance().getRepository().setViewConfig(state);
    }

    public static <S> void saveGlobalState(S state) {
        log.debug("saveGlobalState({})", state.getClass().getSimpleName());
        Application.getInstance().getRepository().setGlobalConfig(state);
    }

    public static void retrieveAllStates() {
        log.debug("retrieveAllStates");

        var application = Application.getInstance();

        for (var plugin : application.plugins) {
            if (plugin.instance() instanceof StateView<?> view) {
                view.restoreState();
            }
        }
    }

    public static void saveAllStates() {
        log.debug("saveAllStates");

        var application = Application.getInstance();

        for (var plugin : application.plugins) {
            if (plugin.instance() instanceof StateView<?> view) {
                view.saveState();
            }
        }
    }

}
