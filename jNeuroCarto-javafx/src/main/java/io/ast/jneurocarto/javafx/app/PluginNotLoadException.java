package io.ast.jneurocarto.javafx.app;

import io.ast.jneurocarto.javafx.view.Plugin;

public class PluginNotLoadException extends Exception {
    public final Class<? extends Plugin> plugin;

    public PluginNotLoadException(Class<? extends Plugin> plugin) {
        super("plugin " + plugin.getName() + " not loaded.");
        this.plugin = plugin;
    }
}
